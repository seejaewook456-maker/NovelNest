package org.example.domain.worldsettingextraction.service;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.airatelimit.service.AiRateLimitService;
import org.example.domain.aiusage.enums.AiFeatureType;
import org.example.domain.aiusage.service.AiUsageService;
import org.example.domain.episode.entity.Episode;
import org.example.domain.episode.repository.EpisodeRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.domain.worldsetting.dto.WorldSettingResponseDto;
import org.example.domain.worldsetting.entity.WorldSetting;
import org.example.domain.worldsetting.repository.WorldSettingRepository;
import org.example.domain.worldsettingextraction.dto.WorldSettingCandidateDto;
import org.example.domain.worldsettingextraction.dto.WorldSettingExtractionResponseDto;
import org.example.domain.worldsettingextraction.dto.WorldSettingNewInsightsDto;
import org.example.global.ai.service.OpenAiService;
import org.example.global.ai.util.AiJsonRepair;
import org.example.global.ai.util.ExtractionChangeDetector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorldSettingExtractionService {

    // 병합 시 기존 내용과 신규 내용 사이를 구분하는 빈 줄 — 반복 보강 시에도 정확히 이 구분자
    // 하나만 남도록 mergeWithExistingContent()가 기존 내용 끝의 개행을 먼저 정리한다.
    private static final String CONTENT_MERGE_SEPARATOR = "\n\n";

    // AI 재구성 설명에 기존 설명의 내용(글자 bigram)이 최소 이 비율 이상 남아 있어야 채택한다.
    // 중복 문장 통합·장황한 표현 정리 같은 재구성은 허용해야 하므로 너무 높게 잡지 않고,
    // 기존 설정을 통째로 빼버린 경우만 걸러낸다. 세부 정보 보존은 핵심 용어(숫자·따옴표 고유명사) 검사가 함께 담당한다.
    private static final double MIN_EXISTING_COVERAGE = 0.6;

    private static final String EXTRACTION_INSTRUCTIONS =
            "당신은 소설 세계관 분석 전문가입니다. 주어진 소설 회차를 분석하여 세계관/설정 후보를 추출합니다.\n\n" +
            "반드시 다음 규칙을 따르세요:\n" +
            "1. 순수 JSON 배열만 반환하세요. 설명, 마크다운 코드블록 등 다른 텍스트는 절대 포함하지 마세요.\n" +
            "2. 기존 세계관 설정 목록이 제공된 경우, 본문에 등장하는 기존 설정을 반드시 식별하세요.\n" +
            "3. 기존 설정은 isExistingSetting을 true로, matchedWorldSettingId에 해당 ID를 설정하세요.\n" +
            "4. 신규 설정은 isExistingSetting을 false로, matchedWorldSettingId를 null로 설정하세요.\n" +
            "4-1. [정보 선별] 세계관 설정은 회차별 사건 기록이 아니라 작가가 이후 회차를 집필할 때 참고하는 설정 자료입니다. " +
            "세계관 관련 내용이 등장했다고 모두 추출하지 말고, 이후 집필에 실질적으로 필요한 정보만 선별하세요.\n" +
            "    - 우선 추출: ① 세계의 작동 방식, 능력·기술의 사용 조건, 제한·금지 사항 ② 중요한 장소의 위치와 역할, " +
            "조직·기관의 기능과 조직 간 관계 ③ 법률·규정, 거래·경제 구조, 계급·권력 관계, 사회적 관습과 제약 " +
            "④ 이후 회차에서도 일관성을 유지해야 하거나, 잊으면 설정 충돌이 생길 수 있는 사실\n" +
            "    - 제외: 특정 회차에서만 일어나는 일시적 사건, 설정과 직접 관련 없는 인물의 행동, 단순한 배경 묘사, " +
            "분위기 조성 문장, 특정 상황에만 해당하는 설명, 이후 집필에 참고할 필요가 낮은 사소한 정보, 기존 설정을 다른 표현으로 반복한 내용\n" +
            "    - 중요도 판단 질문: 이후 집필에 다시 필요할 가능성이 높은가? 기록하지 않으면 설정 충돌이 생길 수 있는가? " +
            "세계관의 규칙·구조·장소·조직 이해에 필요한가? 기존 설명에 없는 실질적인 새 정보인가? 일시적 사건이 아니라 " +
            "지속되는 설정인가? — 모두 충족할 필요는 없으며 종합적으로 판단하되, 사소해 보여도 일관성 유지에 중요하면 추출하세요.\n" +
            "4-2. [요약] 회차 본문 문장을 그대로 복사하거나 지나치게 상세히 쓰지 말고, 문맥을 이해한 뒤 핵심 사실만 간결하게 요약하세요. " +
            "불필요한 묘사·수식어를 빼고, 하나의 사실을 여러 문장으로 반복하지 마세요. 단, 구체적인 규칙·조건·수치·고유명사는 " +
            "반드시 보존하고, 본문에서 확정되지 않은 추측이나 가능성을 확정된 사실처럼 쓰지 마세요. 목표는 '최대한 짧은 설명'이 " +
            "아니라 '필요한 정보는 빠짐없이 담되 불필요하게 길지 않은 설명'입니다. " +
            "(본문: '이안은 북쪽 골목으로 들어섰다. 낡은 건물들이 줄지어 있었고, 좁은 골목마다 정체를 알 수 없는 상인들이 " +
            "기억 포션을 거래하고 있었다. 이곳에서는 공식 거래소와 달리 거래자의 신분을 확인하지 않았고, 거래 내역을 기록하는 " +
            "사람도 없었다.' → 나쁜 예: '북쪽 골목에는 낡은 건물들이 줄지어 있으며 좁은 골목마다 정체를 알 수 없는 상인들이 " +
            "기억 포션을 거래한다...' / 좋은 예: '북쪽 골목에는 신분 확인과 거래 기록 없이 기억을 거래하는 비공식 시장이 형성되어 있다.')\n" +
            "5. newInsights는 기존 설정에만 사용하며, 기존 content에 없는 새로운 정보만 짧은 문장 리스트로 포함하세요. " +
            "기존 설정을 결과에 포함한다면 newInsights는 반드시 1개 이상 작성하세요.\n" +
            "6. evidence는 해당 설정을 뒷받침하는 회차 내 구체적인 장면이나 대사를 기록하세요.\n" +
            "7. category는 반드시 COUNTRY/RACE/MAGIC/ORGANIZATION/PLACE/EVENT/ITEM/RULE/ETC 중 하나로 설정하세요.\n" +
            "8. 모든 문자열 값은 유효한 JSON 문자열이어야 합니다. 값 안에 큰따옴표(\")가 필요하면 반드시 \\\"로 이스케이프하거나 " +
            "작은따옴표(')로 바꿔 쓰세요. 작은따옴표 앞에는 백슬래시(\\)를 붙이지 마세요(\\'는 잘못된 JSON입니다). " +
            "줄바꿈이 필요하면 \\n으로 이스케이프하세요. 대사를 인용할 때 특히 주의하세요. " +
            "반환하기 전에 모든 중괄호 {}와 대괄호 []의 짝이 정확히 맞는지 확인하고, 각 객체는 '}' 하나로만 닫으세요.\n" +
            "9. content(신규 설정) 또는 newContent(기존 설정 보강)를 작성할 때 모든 문장을 하나의 문단으로 이어쓰지 마세요. " +
            "같은 맥락의 문장은 한 문단으로 유지하되, 주제가 전환되거나 서로 다른 규칙·개념으로 넘어갈 때는 문단 사이에 " +
            "빈 줄(\\n\\n)을 넣어 구분하세요. 단, 문장 단위로 무조건 줄바꿈하거나 과도하게 짧은 문단을 여러 개 만들지 말고, " +
            "읽기 자연스러운 문단 구조를 우선하세요.\n" +
            "10. 기존 설정을 보강하는 경우 content에는 기존 content 전체를 새 정보와 함께 다시 분석해 '재구성한 최종 설명 전체'를 " +
            "작성하세요. 기존 설명 뒤에 새 내용을 덧붙이는 방식은 금지합니다. 다음 순서로 작성하세요: 기존 설명 전체 확인 → " +
            "새 정보와 비교 → 중복·유사 정보 식별 → 새 정보를 가장 관련 있는 문장·문단에 통합 → 전체 문맥과 문단 구조 재정리 → " +
            "반복 표현 제거 → 간결하고 자연스러운 최종 설명 완성. 최종 설명은 처음부터 하나의 설정 문서로 작성된 것처럼 읽혀야 하며, " +
            "회차별로 추가된 내용이 나열된 형태가 되어서는 안 됩니다.\n" +
            "    - 기존 설명 안에 같은 의미의 문장이 여러 번 있으면 하나로 통합하고, 비슷한 내용을 다른 표현으로 반복하지 말고, " +
            "관련 정보는 한 문단으로 모으고, 불필요한 수식어와 장황한 설명은 정리하세요. " +
            "(예: '기억 시장에서는 공식 거래와 비공식 거래가 이루어진다. 비공식 거래는 공식 기록에 남지 않는다. 북쪽 골목에서는 " +
            "비공식적인 기억 거래가 이루어진다. 이러한 비공식 거래는 기록이 남지 않아 추적하기 어렵다.' → '기억 시장에서는 공식 " +
            "거래와 비공식 거래가 이루어지며, 특히 북쪽 골목의 비공식 거래는 기록이 남지 않아 유통 경로를 추적하기 어렵다.')\n" +
            "    - 새 정보는 가장 관련 있는 문장에 녹여 넣으세요. (예: 기존 '중앙 기억 거래소는 공식적인 기억 거래를 관리하는 " +
            "시설이다. 거래 과정에서는 신분 확인과 서류 작성이 이루어진다.' + 새 정보 '중앙 거리 끝의 크로아 기억 거래 기록 " +
            "관리소가 거래 기록을 보관한다' → '중앙 기억 거래소는 신분 확인과 서류 작성을 거쳐 공식적인 기억 거래를 관리하는 " +
            "시설이다. 거래 기록은 중앙 거리 끝에 위치한 크로아 기억 거래 기록 관리소에서 보관한다.')\n" +
            "    - 설명을 짧게 만들기 위해 중요한 설정을 삭제하지 마세요. 기존 content의 고유명사·수치·규칙·조건·제한은 " +
            "표현을 다듬더라도 모두 그대로 남기고, 기존 설정의 의미를 바꾸지 마세요.\n" +
            "    - 새 정보가 기존 설정과 충돌(모순)한다면 기존 내용을 덮어쓰지 말고 그 정보는 content에 반영하지 마세요.\n" +
            "    - '추가 내용', '새로운 설정' 같은 소제목이나 구분 표시를 넣지 마세요.\n" +
            "    - newContent에는 기존 content를 반복하지 말고 새 정보만 9번 규칙에 따른 문단으로 작성하세요(서버의 예비 병합용).\n" +
            "10-1. content, newContent, newInsights는 세계관 내부의 사실을 직접 설명하는 문장으로만 작성하세요. " +
            "'이번 회차에서(는)', '이전 회차에서는', '이번에 새롭게 밝혀진', '새롭게 확인된 사실은', '추가된 설정은', " +
            "'기존 설정에 따르면', '이번 내용을 통해', '~라는 사실이 드러났다', '~라는 점이 확인되었다'처럼 회차나 분석 과정, " +
            "정보가 발견된 시점을 설명하는 표현은 절대 쓰지 마세요. " +
            "(나쁜 예: '이번 회차에서는 중앙 거리 끝에 기록 관리소가 실재한다는 사실이 드러났다.' / " +
            "좋은 예: '중앙 거리 끝에는 공식 거래 기록을 보관하고 관리하는 기록 관리소가 위치해 있다.')\n" +
            "11. 기존 설정은 기존 content와 이번 회차 내용을 의미적으로 비교하세요. 기존 설정에 없던 새로운 규칙·특징·역사·배경·조건 " +
            "등 실제로 추가하거나 수정할 정보가 하나도 없다면 그 설정은 결과 배열에 절대 포함하지 마세요. " +
            "(단순히 다시 언급된 경우, 기존 content와 같은 내용, 표현만 다르고 의미가 같은 내용, 기존 content에 이미 포함된 내용은 " +
            "모두 '새로운 정보 없음'입니다. 4-1 기준상 중요도가 낮은 정보만 있는 경우도 '새로운 정보 없음'입니다. " +
            "새 정보가 없다면 문장 개선·표현 정리만을 위해 기존 설정을 결과에 포함하지 마세요.)\n" +
            "12. 본문에서 근거를 찾을 수 없거나 변경 여부가 불확실한 정보는 newInsights/newContent에 넣지 마세요.\n" +
            "13. 결과에 포함할 설정이 하나도 없으면 빈 배열 []을 반환하세요.";

    private final EpisodeRepository episodeRepository;
    private final WorldSettingRepository worldSettingRepository;
    private final UserRepository userRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;
    private final AiUsageService aiUsageService;
    private final AiRateLimitService aiRateLimitService;

    @Transactional(readOnly = true)
    public WorldSettingExtractionResponseDto extractWorldSettings(String email, Long episodeId) {
        User user = findUserByEmail(email);
        Episode episode = findEpisodeById(episodeId);
        Novel novel = episode.getNovel();
        validateOwner(novel, user);

        // 기존 세계관 설정 목록 — AI가 신규/기존 설정을 구분하는 데 사용
        List<WorldSetting> existingSettings = worldSettingRepository.findAllByNovelOrderByCategoryAscIsFavoriteDescTitleAsc(novel);

        // OpenAI 호출 — DB 저장 없음
        String input = buildInput(episode, novel, existingSettings);
        // 분당 Rate Limit → 하루 사용량 제한 순으로 검사한 뒤에만 OpenAI를 호출한다.
        aiRateLimitService.checkAndRecord(user.getId());
        aiUsageService.checkAndIncrement(user.getId(), AiFeatureType.WORLDVIEW_EXTRACTION);
        String aiResponse = openAiService.generateText(EXTRACTION_INSTRUCTIONS, input);
        List<WorldSettingCandidateDto> candidates = parseJson(aiResponse);

        // matchedWorldSettingId로 기존 WorldSetting을 조회해 existingWorldSetting 필드 보강
        Map<Long, WorldSetting> settingMap = existingSettings.stream()
                .collect(Collectors.toMap(WorldSetting::getId, s -> s));

        // 변경사항이 있는 후보만 담는다 — 기존 설정이 새 정보 없이 다시 언급된 경우는 제외
        List<WorldSettingCandidateDto> result = new ArrayList<>();
        for (WorldSettingCandidateDto candidate : candidates) {
            if (Boolean.TRUE.equals(candidate.getIsExistingSetting())
                    && candidate.getMatchedWorldSettingId() != null) {
                WorldSetting matched = settingMap.get(candidate.getMatchedWorldSettingId());
                if (matched != null) {
                    // AI가 규칙을 어기고 기존 내용을 반복했을 수 있으므로 서버에서 한 번 더 걸러낸다
                    if (!removeAlreadyKnownInsights(candidate, matched)) {
                        continue;
                    }
                    candidate.setExistingWorldSetting(WorldSettingResponseDto.from(matched));
                    // 기존 설정의 제목/분류는 보강 대상이 아니므로 AI 값 대신 DB 값을 그대로 쓴다
                    candidate.setTitle(matched.getTitle());
                    candidate.setCategory(matched.getCategory());
                    // AI가 통합한 최종 설명을 검증해 쓰고, 검증에 실패하면 기존 방식(이어 붙이기)으로 대체한다
                    candidate.setContent(resolveIntegratedContent(candidate, matched));
                    result.add(candidate);
                    continue;
                }
            }
            // 신규 설정 — 회차 중심 표현이 섞였을 때를 대비해 문장 앞의 회차 언급만 정리한다
            candidate.setContent(ExtractionChangeDetector.stripEpisodeNarration(candidate.getContent()));
            result.add(candidate);
        }

        log.info("WorldSetting extraction complete. episodeId={}, candidateCount={}, excludedCount={}, userId={}",
                episodeId, result.size(), candidates.size() - result.size(), user.getId());
        String episodeTitle = episode.getEpisodeNumber() + "화 - " + episode.getTitle();
        return new WorldSettingExtractionResponseDto(episodeTitle, result.size(), result);
    }

    // 기존 설정 후보의 newInsights/newContent에서 이미 DB content에 있는 내용을 제거하고,
    // 실제로 추가·보완할 정보가 남아 있으면 true를 반환한다.
    // (DB 엔티티는 읽기만 하고 수정하지 않는다 — 응답 DTO만 정리)
    private boolean removeAlreadyKnownInsights(WorldSettingCandidateDto candidate, WorldSetting existing) {
        String existingContent = existing.getContent();

        List<String> insightItems = candidate.getNewInsights() != null
                ? candidate.getNewInsights().getContent()
                : null;
        List<String> filteredInsights = ExtractionChangeDetector.filterNewItems(insightItems, existingContent);
        candidate.setNewInsights(filteredInsights.isEmpty() ? null : new WorldSettingNewInsightsDto(filteredInsights));

        // newContent가 기존 content에 이미 그대로 포함된 내용이면 병합할 필요가 없으므로 비운다
        if (ExtractionChangeDetector.isAlreadyCovered(candidate.getNewContent(), existingContent)) {
            candidate.setNewContent(null);
        }

        return !filteredInsights.isEmpty() || candidate.getNewContent() != null;
    }

    // 기존 설정 보강 시 최종 content를 결정한다.
    // 1순위: AI가 기존 설명 전체와 새 정보를 함께 재구성한 content — 아래를 모두 통과할 때만 채택
    //   ① 회차 중심 표현("이번 회차에서는" 등)이 없을 것 (문장 앞 회차 표현은 제거 후 재검사)
    //   ② 기존 설명의 내용이 충분히 남아 있을 것 — 중복 통합·간결화는 허용하되 설정을 통째로 빼지 않았는지
    //   ③ 기존 설명의 핵심 용어(숫자, 따옴표로 감싼 고유명사)가 모두 남아 있을 것 — 세부 설정 누락 방지
    //   ④ 기존 설명과 실제로 다를 것 (새 정보가 반영되었는지)
    // 2순위(검증 실패 시, 안전장치): 기존 설명은 DB 값 그대로 두고 새 정보만 빈 줄 뒤에 이어 붙인다
    //   — 추가 AI 호출 없이 사용자가 작성한 기존 설명이 AI 때문에 훼손되는 일을 막는 마지막 경로
    private String resolveIntegratedContent(WorldSettingCandidateDto candidate, WorldSetting existing) {
        String existingContent = existing.getContent() == null ? "" : existing.getContent();
        String integrated = ExtractionChangeDetector.normalizeParagraphs(
                ExtractionChangeDetector.stripEpisodeNarration(candidate.getContent()));

        String rejectReason = null;
        if (integrated.isEmpty()) {
            rejectReason = "empty";
        } else if (ExtractionChangeDetector.containsEpisodeNarration(integrated)) {
            rejectReason = "episode-narration";
        } else if (ExtractionChangeDetector.coverageRatio(existingContent, integrated) < MIN_EXISTING_COVERAGE) {
            rejectReason = "existing-content-lost";
        } else if (!ExtractionChangeDetector.keepsKeyTerms(existingContent, integrated)) {
            rejectReason = "key-term-lost";
        } else if (ExtractionChangeDetector.normalize(integrated).equals(ExtractionChangeDetector.normalize(existingContent))) {
            rejectReason = "no-change";
        }

        if (rejectReason == null) {
            return integrated;
        }

        log.warn("WorldSetting integrated content rejected, fallback to append. worldSettingId={}, reason={}",
                existing.getId(), rejectReason);
        // 예비 병합 — newContent가 없으면 newInsights 문장들을 이어 쓴다
        String addition = candidate.getNewContent();
        if ((addition == null || addition.isBlank()) && candidate.getNewInsights() != null) {
            addition = String.join(" ", candidate.getNewInsights().getContent());
        }
        addition = ExtractionChangeDetector.stripEpisodeNarration(addition);
        if (addition == null || addition.isBlank()) {
            return existingContent;
        }
        return mergeWithExistingContent(existingContent, addition);
    }

    // AI에게 전달하는 입력 텍스트 조립
    private String buildInput(Episode episode, Novel novel, List<WorldSetting> existingSettings) {
        StringBuilder sb = new StringBuilder();

        sb.append("[작품 정보]\n");
        sb.append("제목: ").append(novel.getTitle()).append("\n");
        sb.append("장르: ").append(novel.getGenre() != null ? novel.getGenre() : "없음").append("\n");
        sb.append("설명: ").append(novel.getDescription() != null ? novel.getDescription() : "없음").append("\n\n");

        sb.append("[회차 정보]\n");
        sb.append("회차 번호: ").append(episode.getEpisodeNumber()).append("화\n");
        sb.append("제목: ").append(episode.getTitle()).append("\n");
        sb.append("본문:\n").append(episode.getContent()).append("\n\n");

        sb.append("[기존 세계관 설정 목록]\n");
        if (existingSettings.isEmpty()) {
            sb.append("등록된 세계관 설정이 없습니다.\n");
        } else {
            try {
                List<Map<String, Object>> settingList = existingSettings.stream()
                        .map(s -> {
                            Map<String, Object> map = new LinkedHashMap<>();
                            map.put("id", s.getId());
                            map.put("category", s.getCategory().name());
                            map.put("title", s.getTitle());
                            map.put("content", s.getContent());
                            return map;
                        })
                        .collect(Collectors.toList());
                sb.append(objectMapper.writeValueAsString(settingList)).append("\n");
            } catch (Exception e) {
                sb.append("[]\n");
            }
        }

        sb.append("\n위 회차에서 세계관/설정 후보를 분석하여 아래 형식의 JSON 배열로만 반환하세요:\n");
        sb.append("[{");
        sb.append("\"category\":\"COUNTRY 또는 RACE 또는 MAGIC 또는 ORGANIZATION 또는 PLACE 또는 EVENT 또는 ITEM 또는 RULE 또는 ETC\",");
        sb.append("\"title\":\"설정 제목\",");
        sb.append("\"content\":\"설정 내용 (신규 설정은 전체 내용, 기존 설정 보강은 기존 content와 새 정보를 자연스럽게 통합한 최종 설명 전체)\",");
        sb.append("\"evidence\":\"회차 내 근거 장면 또는 대사\",");
        sb.append("\"isExistingSetting\":true 또는 false,");
        sb.append("\"matchedWorldSettingId\":기존설정ID 또는 null,");
        sb.append("\"newInsights\":{\"content\":[\"새로운 정보1\",\"새로운 정보2\"]} 또는 null,");
        sb.append("\"newContent\":\"기존 설정 보강 시 이번 회차에서 새로 추가되는 문단 내용(문맥 단위 줄바꿈 적용)\" 또는 null");
        sb.append("}]\n");
        sb.append("기존 설정 중 새로 추가·수정할 정보가 없는 설정은 배열에서 제외하고, 포함할 설정이 없으면 []만 반환하세요.");

        return sb.toString();
    }

    // 기존 세계관 내용 뒤에 신규 보강 내용을 정확히 빈 줄 하나로 구분해 붙인다.
    // 기존 내용 끝의 개행/공백을 먼저 제거한 뒤 구분자를 붙이므로, 여러 회차에 걸쳐 반복
    // 보강해도 빈 줄이 2개 이상으로 누적되지 않는다.
    private String mergeWithExistingContent(String existingContent, String newContent) {
        String trimmedExisting = existingContent == null ? "" : stripTrailingBlank(existingContent);
        String trimmedNew = newContent.strip();

        if (trimmedExisting.isEmpty()) {
            return trimmedNew;
        }
        return trimmedExisting + CONTENT_MERGE_SEPARATOR + trimmedNew;
    }

    // 문자열 끝의 개행/공백 문자만 제거 (앞부분과 중간 내용은 그대로 보존)
    private String stripTrailingBlank(String text) {
        int end = text.length();
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }

    // OpenAI 응답 텍스트 → List<WorldSettingCandidateDto> 파싱
    private List<WorldSettingCandidateDto> parseJson(String jsonText) {
        if (jsonText == null || jsonText.isBlank()) {
            throw new IllegalArgumentException("AI 응답이 비어 있습니다. 다시 시도해 주세요.");
        }

        // AI가 마크다운 코드블록(```json ... ```)을 포함하는 경우 제거
        String cleaned = jsonText.replaceAll("(?s)```json\\s*|```\\s*", "").trim();

        // AI가 대사를 인용하며 JSON에서 허용되지 않는 이스케이프(\' 등)를 쓰는 경우가 있어,
        // 이 응답을 읽을 때만 "백슬래시 뒤 임의 문자"를 허용한다 (\' → '). 공용 ObjectMapper 설정은 바꾸지 않는다.
        ObjectReader reader = objectMapper.readerFor(new TypeReference<List<WorldSettingCandidateDto>>() {})
                .with(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER);

        try {
            return reader.readValue(cleaned);
        } catch (Exception firstError) {
            // AI가 괄호 짝을 틀리는 경우(예: 객체를 닫은 뒤 '}'를 하나 더 씀)가 있어
            // 문자열 밖의 괄호 짝만 맞춰 한 번 더 시도한다. 그래도 실패하면 기존과 동일하게 오류로 처리한다.
            log.warn("WorldSetting AI response parse failed, retrying with bracket repair. length={}, cause={}",
                    cleaned.length(), firstError.getMessage());
            log.debug("WorldSetting AI raw response: {}", cleaned);
            try {
                return reader.readValue(AiJsonRepair.balanceBrackets(cleaned));
            } catch (Exception e) {
                throw new IllegalArgumentException("AI 응답 파싱에 실패했습니다. 다시 시도해 주세요. 원인: " + firstError.getMessage());
            }
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private Episode findEpisodeById(Long episodeId) {
        return episodeRepository.findById(episodeId)
                .orElseThrow(() -> new IllegalArgumentException("회차를 찾을 수 없습니다."));
    }

    // episode → novel → user 체인으로 소유권 검증
    private void validateOwner(Novel novel, User user) {
        if (!novel.getUser().getId().equals(user.getId())) {
            throw new SecurityException("해당 회차에 대한 권한이 없습니다.");
        }
    }
}
