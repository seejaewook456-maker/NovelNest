package org.example.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.character.entity.Character;
import org.example.domain.character.repository.CharacterRepository;
import org.example.domain.chat.dto.ChatResponseDto;
import org.example.domain.chat.dto.ContextStatsDto;
import org.example.domain.episode.repository.EpisodeRepository;
import org.example.domain.episodesummary.entity.EpisodeSummary;
import org.example.domain.episodesummary.repository.EpisodeSummaryRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.novel.repository.NovelRepository;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.domain.worldsetting.entity.WorldSetting;
import org.example.domain.worldsetting.repository.WorldSettingRepository;
import org.example.global.ai.service.OpenAiService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    // 작가 맞춤형 AI 비서 — 창작 존중, 정보 공백 솔직 인정, Markdown 형식 답변
    private static final String CHAT_INSTRUCTIONS =
            "당신은 작가의 AI 글쓰기 비서입니다. " +
            "집필 중인 소설의 등장인물, 세계관, 회차 요약 정보를 바탕으로 작가의 질문에 성실하게 답합니다.\n\n" +
            "[반드시 지켜야 할 규칙]\n" +
            "1. 제공된 정보에 없는 내용은 절대 추측하거나 만들어내지 마세요. 모르는 내용은 솔직히 모른다고 말하세요.\n" +
            "2. 회차 요약이 없는 회차의 내용은 알 수 없다고 명시하세요. 요약이 있는 회차만 참고하세요.\n" +
            "3. 작가의 창작 방향을 존중하세요. 수정을 강요하지 말고 '~하면 어떨까요?' 같은 제안 형식을 사용하세요.\n" +
            "4. 답변은 Markdown 형식으로 작성하세요. 항목이 여러 개일 때는 목록(- 또는 번호)을 활용하세요.\n" +
            "5. 등장인물과 세계관 설정을 적극적으로 인용해 구체적으로 답하세요.\n" +
            "6. 한국어로만 답하세요.\n" +
            "7. 작가에게 친근하고 격려하는 톤으로 말하되, 전문적인 피드백을 제공하세요.";

    private final NovelRepository novelRepository;
    private final CharacterRepository characterRepository;
    private final WorldSettingRepository worldSettingRepository;
    private final EpisodeSummaryRepository episodeSummaryRepository;
    private final EpisodeRepository episodeRepository;
    private final UserRepository userRepository;
    private final OpenAiService openAiService;

    // 작가 질문에 AI가 컨텍스트를 바탕으로 답변
    @Transactional(readOnly = true)
    public ChatResponseDto chat(String email, Long novelId, String message) {
        User user = findUserByEmail(email);
        Novel novel = findNovelById(novelId);
        validateOwner(novel, user);

        List<Character> characters = characterRepository.findAllByNovelOrderByNameAsc(novel);
        List<WorldSetting> worldSettings = worldSettingRepository.findAllByNovelOrderByCategoryAscTitleAsc(novel);
        List<EpisodeSummary> summaries = episodeSummaryRepository.findAllSummariesByNovel(novel);
        long totalEpisodeCount = episodeRepository.countByNovel(novel);

        String input = buildContext(novel, characters, worldSettings, summaries, totalEpisodeCount, message);
        String answer = openAiService.generateText(CHAT_INSTRUCTIONS, input);

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
    private String buildContext(Novel novel, List<Character> characters,
                                List<WorldSetting> worldSettings,
                                List<EpisodeSummary> summaries,
                                long totalEpisodeCount, String message) {
        StringBuilder sb = new StringBuilder();

        // 작품 기본 정보
        sb.append("[작품 정보]\n");
        sb.append("제목: ").append(novel.getTitle()).append("\n");
        sb.append("장르: ").append(novel.getGenre() != null ? novel.getGenre() : "없음").append("\n");
        sb.append("설명: ").append(novel.getDescription() != null ? novel.getDescription() : "없음").append("\n\n");

        // 등장인물 목록
        sb.append("[등장인물 목록 - 총 ").append(characters.size()).append("명]\n");
        if (characters.isEmpty()) {
            sb.append("등록된 등장인물이 없습니다.\n");
        } else {
            for (Character c : characters) {
                sb.append("• ").append(c.getName());
                if (c.getRole() != null && !c.getRole().isBlank()) {
                    sb.append(" (").append(c.getRole()).append(")");
                }
                if (c.getAge() != null) {
                    sb.append(", ").append(c.getAge()).append("세");
                }
                sb.append("\n");
                if (c.getPersonality() != null && !c.getPersonality().isBlank()) {
                    sb.append("  성격: ").append(c.getPersonality()).append("\n");
                }
                if (c.getSpeechStyle() != null && !c.getSpeechStyle().isBlank()) {
                    sb.append("  말투: ").append(c.getSpeechStyle()).append("\n");
                }
                if (c.getDescription() != null && !c.getDescription().isBlank()) {
                    sb.append("  설명: ").append(c.getDescription()).append("\n");
                }
            }
        }
        sb.append("\n");

        // 세계관 설정 목록
        sb.append("[세계관 설정 - 총 ").append(worldSettings.size()).append("개]\n");
        if (worldSettings.isEmpty()) {
            sb.append("등록된 세계관 설정이 없습니다.\n");
        } else {
            for (WorldSetting ws : worldSettings) {
                sb.append("[").append(ws.getCategory().name()).append("] ")
                  .append(ws.getTitle()).append("\n");
                sb.append(ws.getContent()).append("\n");
            }
        }
        sb.append("\n");

        // 회차 요약 목록 — 요약이 있는 회차만 포함
        sb.append("[회차 요약 - ").append(summaries.size()).append("/")
          .append(totalEpisodeCount).append(" 회차 요약 존재]\n");
        if (summaries.isEmpty()) {
            sb.append("작성된 회차 요약이 없습니다.\n");
        } else {
            for (EpisodeSummary es : summaries) {
                sb.append(es.getEpisode().getEpisodeNumber()).append("화 - ")
                  .append(es.getEpisode().getTitle()).append(": ")
                  .append(es.getSummary()).append("\n");
            }
            if (summaries.size() < totalEpisodeCount) {
                sb.append("※ 요약이 없는 회차의 내용은 위에 포함되지 않았습니다.\n");
            }
        }
        sb.append("\n---\n\n작가의 질문:\n").append(message);

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
