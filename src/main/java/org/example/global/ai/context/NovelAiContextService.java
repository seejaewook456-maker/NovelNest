package org.example.global.ai.context;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.character.entity.Character;
import org.example.domain.character.repository.CharacterRepository;
import org.example.domain.episodesummary.entity.EpisodeSummary;
import org.example.domain.episodesummary.repository.EpisodeSummaryRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.worldsetting.entity.WorldSetting;
import org.example.domain.worldsetting.repository.WorldSettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

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

    // AI 채팅용 — 작품의 가장 최근 회차 요약만 설정된 개수만큼 사용
    @Transactional(readOnly = true)
    public NovelAiContext buildForChat(Novel novel) {
        List<EpisodeSummary> recentDesc = episodeSummaryRepository
                .findRecentSummariesByNovel(novel, PageRequest.of(0, chatRecentEpisodeCount));
        return build(novel, recentDesc, "CHAT");
    }

    // 설정 충돌 감지용 — 검사 대상 회차보다 이전 회차의 요약만 설정된 개수만큼 사용
    @Transactional(readOnly = true)
    public NovelAiContext buildForConflictDetection(Novel novel, int targetEpisodeNumber) {
        List<EpisodeSummary> recentDesc = episodeSummaryRepository
                .findRecentSummariesBeforeEpisode(novel, targetEpisodeNumber, PageRequest.of(0, conflictRecentEpisodeCount));
        return build(novel, recentDesc, "CONFLICT_DETECTION");
    }

    private NovelAiContext build(Novel novel, List<EpisodeSummary> recentSummariesDesc, String purpose) {
        List<Character> characters = characterRepository.findAllByNovelOrderByIsFavoriteDescNameAsc(novel);
        List<WorldSetting> worldSettings = worldSettingRepository.findAllByNovelOrderByCategoryAscIsFavoriteDescTitleAsc(novel);

        List<Character> limitedCharacters = limitCharacters(characters);
        List<WorldSetting> limitedWorldSettings = limitWorldSettings(worldSettings);

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

    // 즐겨찾기 우선, 초과분은 최근 수정 순으로 채움 (섹션 7: 즐겨찾기 > 최근 수정 > 나머지)
    private List<Character> limitCharacters(List<Character> characters) {
        if (characters.size() <= maxCharacterCount) {
            return characters;
        }
        List<Character> favorites = characters.stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsFavorite()))
                .toList();
        List<Character> rest = characters.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsFavorite()))
                .sorted(Comparator.comparing(Character::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return Stream.concat(favorites.stream(), rest.stream())
                .limit(maxCharacterCount)
                .toList();
    }

    // 즐겨찾기 우선, 초과분은 최근 수정 순으로 채움 (섹션 8)
    private List<WorldSetting> limitWorldSettings(List<WorldSetting> worldSettings) {
        if (worldSettings.size() <= maxWorldviewCount) {
            return worldSettings;
        }
        List<WorldSetting> favorites = worldSettings.stream()
                .filter(w -> Boolean.TRUE.equals(w.getIsFavorite()))
                .toList();
        List<WorldSetting> rest = worldSettings.stream()
                .filter(w -> !Boolean.TRUE.equals(w.getIsFavorite()))
                .sorted(Comparator.comparing(WorldSetting::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return Stream.concat(favorites.stream(), rest.stream())
                .limit(maxWorldviewCount)
                .toList();
    }

    // 섹션 9: 즐겨찾기 인물 → 즐겨찾기 세계관 → 직전 회차 요약 → 나머지 인물 → 나머지 세계관 순으로
    // 항목 단위로 채우다가 문자 예산을 넘기면 그 뒤 항목은 전부 제외한다 (중간에서 문자열을 자르지 않음).
    private BudgetResult applyBudget(List<Character> characters, List<WorldSetting> worldSettings,
                                      List<EpisodeSummary> summariesAsc) {
        List<Character> favoriteCharacters = characters.stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsFavorite())).toList();
        List<Character> restCharacters = characters.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsFavorite())).toList();
        List<WorldSetting> favoriteWorldSettings = worldSettings.stream()
                .filter(w -> Boolean.TRUE.equals(w.getIsFavorite())).toList();
        List<WorldSetting> restWorldSettings = worldSettings.stream()
                .filter(w -> !Boolean.TRUE.equals(w.getIsFavorite())).toList();

        List<CharacterContext> includedCharacters = new ArrayList<>();
        List<WorldSettingContext> includedWorldSettings = new ArrayList<>();
        List<EpisodeSummaryContext> includedSummaries = new ArrayList<>();

        int[] remaining = {maxContextCharacters};
        int[] excludedCount = {0};
        boolean[] budgetExhausted = {false};

        addCharacters(favoriteCharacters, includedCharacters, remaining, excludedCount, budgetExhausted);
        addWorldSettings(favoriteWorldSettings, includedWorldSettings, remaining, excludedCount, budgetExhausted);
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
