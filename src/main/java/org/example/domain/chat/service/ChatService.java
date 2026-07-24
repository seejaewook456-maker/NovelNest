package org.example.domain.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.chat.dto.ChatResponseDto;
import org.example.domain.chat.dto.ContextStatsDto;
import org.example.domain.character.repository.CharacterRepository;
import org.example.domain.episode.repository.EpisodeRepository;
import org.example.domain.episodesummary.repository.EpisodeSummaryRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.novel.repository.NovelRepository;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.domain.worldsetting.repository.WorldSettingRepository;
import org.example.global.ai.context.CharacterContext;
import org.example.global.ai.context.EpisodeSummaryContext;
import org.example.global.ai.context.NovelAiContext;
import org.example.global.ai.context.NovelAiContextService;
import org.example.global.ai.context.WorldSettingContext;
import org.example.global.ai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    // 작가 맞춤형 AI 비서 — 창작 존중, 정보 공백 솔직 인정, Markdown 형식 답변
    private static final String CHAT_INSTRUCTIONS =
            "당신은 작가의 AI 글쓰기 비서입니다. " +
            "집필 중인 소설의 등장인물, 세계관, 최근 회차 요약 정보를 바탕으로 작가의 질문에 성실하게 답합니다.\n\n" +
            "[반드시 지켜야 할 규칙]\n" +
            "1. 제공된 정보에 없는 내용은 절대 추측하거나 만들어내지 마세요. 확인할 수 없는 내용은 솔직히 모른다고 말하세요.\n" +
            "2. 작품 외부의 일반 지식을 작품 설정인 것처럼 섞어서 답하지 마세요.\n" +
            "3. 최근 상황에 대한 질문에는 [최근 회차 요약]을 우선 참고하세요.\n" +
            "4. 인물의 고정된 설정에 대한 질문에는 [작품의 등장인물 정보]를 우선 참고하세요.\n" +
            "5. 세계관에 대한 질문에는 [작품의 세계관 정보]를 우선 참고하세요.\n" +
            "6. 서로 다른 참고 정보가 충돌하는 것처럼 보이면 단정하지 말고 모순 가능성이 있다고 알려주세요.\n" +
            "7. 가능한 경우 답변에서 참고한 회차 번호를 표시하세요 (예: 12화 참고).\n" +
            "8. 작가의 창작 방향을 존중하세요. 수정을 강요하지 말고 '~하면 어떨까요?' 같은 제안 형식을 사용하세요.\n" +
            "9. 답변은 Markdown 형식으로 작성하세요. 항목이 여러 개일 때는 목록(- 또는 번호)을 활용하세요.\n" +
            "10. 한국어로만, 친근하고 격려하는 톤으로 답하되 전문적인 피드백을 제공하세요.\n\n" +
            "이 기능은 작품 설정을 참고해서 답하는 읽기 전용 비서입니다. 작품 데이터를 직접 수정하지 않습니다.";

    private final NovelRepository novelRepository;
    private final CharacterRepository characterRepository;
    private final WorldSettingRepository worldSettingRepository;
    private final EpisodeSummaryRepository episodeSummaryRepository;
    private final EpisodeRepository episodeRepository;
    private final UserRepository userRepository;
    private final NovelAiContextService novelAiContextService;
    private final OpenAiService openAiService;

    @Value("${app.ai.chat.max-output-tokens}")
    private int chatMaxOutputTokens;

    // 작가 질문에 AI가 컨텍스트를 바탕으로 답변
    @Transactional(readOnly = true)
    public ChatResponseDto chat(String email, Long novelId, String message) {
        User user = findUserByEmail(email);
        Novel novel = findNovelById(novelId);
        validateOwner(novel, user);

        NovelAiContext context = novelAiContextService.buildForChat(novel);

        log.info("Chat request. novelId={}, userId={}", novelId, user.getId());
        String input = buildContext(novel, context, message);
        String answer = openAiService.generateText(CHAT_INSTRUCTIONS, input, chatMaxOutputTokens);

        return new ChatResponseDto(answer);
    }

    // 챗봇 섹션 상단에 표시할 데이터 현황 조회 — 전체 데이터 로드 없이 count 쿼리만 실행
    @Transactional(readOnly = true)
    public ContextStatsDto getContextStats(String email, Long novelId) {
        User user = findUserByEmail(email);
        Novel novel = findNovelById(novelId);
        validateOwner(novel, user);

        long totalEpisodeCount = episodeRepository.countByNovel(novel);
        long summaryCount = episodeSummaryRepository.countSummariesByNovel(novel);
        long characterCount = characterRepository.countByNovel(novel);
        long worldSettingCount = worldSettingRepository.countByNovel(novel);

        return new ContextStatsDto(totalEpisodeCount, summaryCount, characterCount, worldSettingCount);
    }

    // AI에게 전달할 컨텍스트 텍스트를 조립
    private String buildContext(Novel novel, NovelAiContext context, String message) {
        StringBuilder sb = new StringBuilder();

        // 작품 기본 정보
        sb.append("[작품 정보]\n");
        sb.append("제목: ").append(novel.getTitle()).append("\n");
        sb.append("장르: ").append(novel.getGenre() != null ? novel.getGenre() : "없음").append("\n");
        sb.append("설명: ").append(novel.getDescription() != null ? novel.getDescription() : "없음").append("\n\n");

        // 등장인물 정보
        List<CharacterContext> characters = context.characters();
        sb.append("[작품의 등장인물 정보]\n");
        if (characters.isEmpty()) {
            sb.append("등록된 등장인물 정보 없음\n");
        } else {
            for (CharacterContext c : characters) {
                sb.append("• ").append(c.name());
                if (c.role() != null) {
                    sb.append(" (").append(c.role()).append(")");
                }
                if (c.age() != null) {
                    sb.append(", ").append(c.age()).append("세");
                }
                sb.append("\n");
                if (c.personality() != null) {
                    sb.append("  성격: ").append(c.personality()).append("\n");
                }
                if (c.speechStyle() != null) {
                    sb.append("  말투: ").append(c.speechStyle()).append("\n");
                }
                if (c.description() != null) {
                    sb.append("  설명: ").append(c.description()).append("\n");
                }
            }
        }
        sb.append("\n");

        // 세계관 정보 — 카테고리별로 묶어서 전달
        List<WorldSettingContext> worldSettings = context.worldSettings();
        sb.append("[작품의 세계관 정보]\n");
        if (worldSettings.isEmpty()) {
            sb.append("등록된 세계관 정보 없음\n");
        } else {
            String currentCategory = null;
            for (WorldSettingContext ws : worldSettings) {
                if (!ws.category().equals(currentCategory)) {
                    currentCategory = ws.category();
                    sb.append("[").append(currentCategory).append("]\n");
                }
                sb.append("- ").append(ws.title());
                if (ws.content() != null) {
                    sb.append(": ").append(ws.content());
                }
                sb.append("\n");
            }
        }
        sb.append("\n");

        // 최근 회차 요약 — 작품의 모든 회차 요약이 아니라 최근 N개만 (설정값 기준)
        List<EpisodeSummaryContext> summaries = context.episodeSummaries();
        sb.append("[최근 회차 요약]\n");
        if (summaries.isEmpty()) {
            sb.append("작성된 회차 요약이 없습니다.\n");
        } else {
            for (EpisodeSummaryContext es : summaries) {
                sb.append(es.episodeNumber()).append("화 - ")
                  .append(es.episodeTitle()).append(": ")
                  .append(es.summary()).append("\n");
            }
        }

        sb.append("\n---\n\n[사용자 질문]\n").append(message);

        return sb.toString();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private Novel findNovelById(Long novelId) {
        return novelRepository.findById(novelId)
                .orElseThrow(() -> new IllegalArgumentException("작품을 찾을 수 없습니다."));
    }

    private void validateOwner(Novel novel, User user) {
        if (!novel.getUser().getId().equals(user.getId())) {
            throw new SecurityException("해당 작품에 대한 권한이 없습니다.");
        }
    }
}
