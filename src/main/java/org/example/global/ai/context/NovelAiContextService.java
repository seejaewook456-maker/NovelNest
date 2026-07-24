package org.example.global.ai.context;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.character.entity.Character;
import org.example.domain.character.repository.CharacterRepository;
import org.example.domain.episode.entity.Episode;
import org.example.domain.episodesummary.entity.EpisodeSummary;
import org.example.domain.episodesummary.repository.EpisodeSummaryRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.worldsetting.entity.WorldSetting;
import org.example.domain.worldsetting.repository.WorldSettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

// AI 채팅과 설정 충돌 감지가 공통으로 사용하는 작품 참고 데이터 구성 로직.
// - 회차 요약은 항상 DB 쿼리 단계에서 "최근 N개"만 조회한다 (전체 조회 후 자르기 금지).
// - 등장인물/세계관/요약을 엔티티 그대로 넘기지 않고 DTO로 변환한다.
// - 전체 참고 데이터가 문자 수 예산을 넘으면 우선순위가 낮은 항목부터 통째로 제외한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class NovelAiContextService {

    // 필드 하나가 지나치게 길어 예산을 독식하지 않도록 하는 안전장치.
    // 요청 사양에 별도 설정 키가 없어 상수로 고정한다.
    private static final int MAX_FIELD_LENGTH = 300;

    // 등장인물/세계관 우선순위 점수 — 현재 회차 언급 + 즐겨찾기면 두 점수가 합산된다.
    private static final int MENTION_SCORE = 100;
    private static final int FAVORITE_SCORE = 50;

    private final CharacterRepository characterRepository;
    private final WorldSettingRepository worldSettingRepository;
    private final EpisodeSummaryRepository episodeSummaryRepository;

    @Value("${app.ai.context.chat-recent-episode-count}")
    private int chatRecentEpisodeCount;

    @Value("${app.ai.context.conflict-recent-episode-count}")
    private int conflictRecentEpisodeCount;

    @Value("${app.ai.context.max-character-count}")
    private int maxCharacterCount;

    @Value("${app.ai.context.max-worldview-count}")
    private int maxWorldviewCount;

    @Value("${app.ai.context.max-context-characters}")
    private int maxContextCharacters;

    // AI 채팅용 — 작품의 가장 최근 회차 요약만 설정된 개수만큼 사용.
    // 채팅은 특정 회차에 매인 기능이 아니라 "현재 회차 언급 여부"를 판단할 대상이 없으므로
    // episodeContent를 null로 넘긴다 — 이 경우 즐겨찾기 우선 + 나머지는 최근 수정순으로 동작한다(기존과 동일).
    @Transactional(readOnly = true)
    public NovelAiContext buildForChat(Novel novel) {
        List<EpisodeSummary> recentDesc = episodeSummaryRepository
                .findRecentSummariesByNovel(novel, PageRequest.of(0, chatRecentEpisodeCount));
        return build(novel, recentDesc, "CHAT", null);
    }

    // 설정 충돌 감지용 — 검사 대상 회차보다 이전 회차의 요약만 설정된 개수만큼 사용.
    // 등장인물/세계관 우선순위 계산에 검사 대상 회차 본문(targetEpisode.getContent())을 사용한다.
    @Transactional(readOnly = true)
    public NovelAiContext buildForConflictDetection(Novel novel, Episode targetEpisode) {
        List<EpisodeSummary> recentDesc = episodeSummaryRepository
                .findRecentSummariesBeforeEpisode(novel, targetEpisode.getEpisodeNumber(), PageRequest.of(0, conflictRecentEpisodeCount));
        return build(novel, recentDesc, "CONFLICT_DETECTION", targetEpisode.getContent());
    }

    private NovelAiContext build(Novel novel, List<EpisodeSummary> recentSummariesDesc, String purpose, String episodeContent) {
        List<Character> characters = characterRepository.findAllByNovelOrderByIsFavoriteDescNameAsc(novel);
        List<WorldSetting> worldSettings = worldSettingRepository.findAllByNovelOrderByCategoryAscIsFavoriteDescTitleAsc(novel);

        // 우선순위 정렬 → 최대 개수 제한 (현재 회차 언급 + 즐겨찾기 > 언급 > 즐겨찾기 > 나머지(최근 수정순))
        List<ScoredItem<Character>> limitedCharacters = prioritize(characters, maxCharacterCount, episodeContent,
                Character::getName, Character::getIsFavorite, Character::getUpdatedAt);
        List<ScoredItem<WorldSetting>> limitedWorldSettings = prioritize(worldSettings, maxWorldviewCount, episodeContent,
                WorldSetting::getTitle, WorldSetting::getIsFavorite, WorldSetting::getUpdatedAt);

        // 프롬프트에는 항상 시간순(과거 → 최신)으로 제공
        List<EpisodeSummary> summariesAsc = recentSummariesDesc.stream()
                .sorted(Comparator.comparingInt(es -> es.getEpisode().getEpisodeNumber()))
                .toList();

        BudgetResult result = applyBudget(limitedCharacters, limitedWorldSettings, summariesAsc);

        log.info("AI context built. purpose={}, novelId={}, characterCount={}, worldSettingCount={}, " +
                        "summaryCount={}, totalContextChars={}, excludedItemCount={}",
                purpose, novel.getId(), result.characters().size(), result.worldSettings().size(),
                result.summaries().size(), result.totalChars(), result.excludedCount());

        return new NovelAiContext(result.characters(), result.worldSettings(), result.summaries());
    }

    // 등장인물/세계관 공통 우선순위 정책: (현재 회차 언급 여부 + 즐겨찾기 여부)로 점수를 매겨 내림차순 정렬하고,
    // 동점이면 최근 수정순(updatedAt DESC)으로 정렬한 뒤 최대 개수만큼만 남긴다.
    // 두 엔티티 타입이 필드명은 다르지만 같은 정책을 쓰므로 추출 함수를 파라미터로 받아 중복을 없앤다.
    // 점수를 그대로 반환하는 이유: applyBudget()이 문자 예산 우선순위(즐겨찾기 티어)를 나눌 때도
    // 이 점수를 기준으로 삼아야 정렬 결과와 예산 배정 우선순위가 어긋나지 않는다.
    private <T> List<ScoredItem<T>> prioritize(List<T> items, int maxCount, String episodeContent,
                                                Function<T, String> nameFn, Function<T, Boolean> favoriteFn,
                                                Function<T, LocalDateTime> updatedAtFn) {
        return items.stream()
                .map(item -> new ScoredItem<>(item, score(nameFn.apply(item), favoriteFn.apply(item), episodeContent), updatedAtFn.apply(item)))
                .sorted(Comparator.<ScoredItem<T>>comparingInt(ScoredItem::score).reversed()
                        .thenComparing(ScoredItem::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(maxCount)
                .toList();
    }

    private int score(String name, Boolean favorite, String episodeContent) {
        int score = 0;
        if (isMentioned(episodeContent, name)) {
            score += MENTION_SCORE;
        }
        if (Boolean.TRUE.equals(favorite)) {
            score += FAVORITE_SCORE;
        }
        return score;
    }

    // MVP 단계: 이름/제목 문자열이 회차 본문에 그대로 포함되는지만 확인한다 (별칭·AI 분석 없음).
    private boolean isMentioned(String episodeContent, String name) {
        return episodeContent != null && !episodeContent.isBlank()
                && name != null && !name.isBlank()
                && episodeContent.contains(name);
    }

    private record ScoredItem<T>(T item, int score, LocalDateTime updatedAt) {
    }

    // 즐겨찾기 인물 → 즐겨찾기 세계관 → 직전 회차 요약 → 나머지 인물 → 나머지 세계관 순으로
    // 항목 단위로 채우다가 문자 예산을 넘기면 그 뒤 항목은 전부 제외한다 (중간에서 문자열을 자르지 않음).
    // "즐겨찾기 티어" 여부는 이제 isFavorite 단독이 아니라 prioritize()가 매긴 점수(score > 0,
    // 즉 현재 회차 언급 또는 즐겨찾기)로 판단한다 — 그래야 정렬 순서와 예산 배정 우선순위가 일치한다.
    // 각 리스트는 이미 prioritize()에서 우선순위 정렬이 끝난 상태이므로 filter로도 상대 순서가 유지된다.
    private BudgetResult applyBudget(List<ScoredItem<Character>> characters, List<ScoredItem<WorldSetting>> worldSettings,
                                      List<EpisodeSummary> summariesAsc) {
        List<Character> priorityCharacters = characters.stream()
                .filter(s -> s.score() > 0).map(ScoredItem::item).toList();
        List<Character> restCharacters = characters.stream()
                .filter(s -> s.score() == 0).map(ScoredItem::item).toList();
        List<WorldSetting> priorityWorldSettings = worldSettings.stream()
                .filter(s -> s.score() > 0).map(ScoredItem::item).toList();
        List<WorldSetting> restWorldSettings = worldSettings.stream()
                .filter(s -> s.score() == 0).map(ScoredItem::item).toList();

        List<CharacterContext> includedCharacters = new ArrayList<>();
        List<WorldSettingContext> includedWorldSettings = new ArrayList<>();
        List<EpisodeSummaryContext> includedSummaries = new ArrayList<>();

        int[] remaining = {maxContextCharacters};
        int[] excludedCount = {0};
        boolean[] budgetExhausted = {false};

        addCharacters(priorityCharacters, includedCharacters, remaining, excludedCount, budgetExhausted);
        addWorldSettings(priorityWorldSettings, includedWorldSettings, remaining, excludedCount, budgetExhausted);
        addSummaries(summariesAsc, includedSummaries, remaining, excludedCount, budgetExhausted);
        addCharacters(restCharacters, includedCharacters, remaining, excludedCount, budgetExhausted);
        addWorldSettings(restWorldSettings, includedWorldSettings, remaining, excludedCount, budgetExhausted);

        int totalChars = maxContextCharacters - remaining[0];
        return new BudgetResult(includedCharacters, includedWorldSettings, includedSummaries,
                totalChars, excludedCount[0]);
    }

    private void addCharacters(List<Character> source, List<CharacterContext> target,
                                int[] remaining, int[] excludedCount, boolean[] budgetExhausted) {
        for (Character c : source) {
            if (budgetExhausted[0]) {
                excludedCount[0]++;
                continue;
            }
            CharacterContext ctx = new CharacterContext(
                    c.getName(), truncate(c.getRole()), c.getAge(),
                    truncate(c.getPersonality()), truncate(c.getSpeechStyle()), truncate(c.getDescription()));
            int length = length(ctx);
            if (length > remaining[0]) {
                budgetExhausted[0] = true;
                excludedCount[0]++;
                continue;
            }
            target.add(ctx);
            remaining[0] -= length;
        }
    }

    private void addWorldSettings(List<WorldSetting> source, List<WorldSettingContext> target,
                                   int[] remaining, int[] excludedCount, boolean[] budgetExhausted) {
        for (WorldSetting w : source) {
            if (budgetExhausted[0]) {
                excludedCount[0]++;
                continue;
            }
            WorldSettingContext ctx = new WorldSettingContext(
                    w.getCategory().name(), w.getTitle(), truncate(w.getContent()));
            int length = length(ctx);
            if (length > remaining[0]) {
                budgetExhausted[0] = true;
                excludedCount[0]++;
                continue;
            }
            target.add(ctx);
            remaining[0] -= length;
        }
    }

    private void addSummaries(List<EpisodeSummary> source, List<EpisodeSummaryContext> target,
                               int[] remaining, int[] excludedCount, boolean[] budgetExhausted) {
        for (EpisodeSummary es : source) {
            if (budgetExhausted[0]) {
                excludedCount[0]++;
                continue;
            }
            EpisodeSummaryContext ctx = new EpisodeSummaryContext(
                    es.getEpisode().getEpisodeNumber(), es.getEpisode().getTitle(), truncate(es.getSummary()));
            int length = length(ctx);
            if (length > remaining[0]) {
                budgetExhausted[0] = true;
                excludedCount[0]++;
                continue;
            }
            target.add(ctx);
            remaining[0] -= length;
        }
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() > MAX_FIELD_LENGTH ? value.substring(0, MAX_FIELD_LENGTH) + "…" : value;
    }

    private int length(CharacterContext c) {
        return safeLen(c.name()) + safeLen(c.role()) + safeLen(c.personality())
                + safeLen(c.speechStyle()) + safeLen(c.description());
    }

    private int length(WorldSettingContext w) {
        return safeLen(w.category()) + safeLen(w.title()) + safeLen(w.content());
    }

    private int length(EpisodeSummaryContext e) {
        return safeLen(e.episodeTitle()) + safeLen(e.summary());
    }

    private int safeLen(String value) {
        return value == null ? 0 : value.length();
    }

    private record BudgetResult(
            List<CharacterContext> characters,
            List<WorldSettingContext> worldSettings,
            List<EpisodeSummaryContext> summaries,
            int totalChars,
            int excludedCount
    ) {
    }
}
