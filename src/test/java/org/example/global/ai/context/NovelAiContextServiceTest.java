package org.example.global.ai.context;

import org.example.domain.character.entity.Character;
import org.example.domain.character.repository.CharacterRepository;
import org.example.domain.episode.entity.Episode;
import org.example.domain.episodesummary.entity.EpisodeSummary;
import org.example.domain.episodesummary.repository.EpisodeSummaryRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.worldsetting.repository.WorldSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NovelAiContextServiceTest {

    @Mock
    private CharacterRepository characterRepository;

    @Mock
    private WorldSettingRepository worldSettingRepository;

    @Mock
    private EpisodeSummaryRepository episodeSummaryRepository;

    private NovelAiContextService novelAiContextService;

    private static final int CHAT_RECENT_EPISODE_COUNT = 10;
    private static final int CONFLICT_RECENT_EPISODE_COUNT = 5;
    private static final int MAX_CHARACTER_COUNT = 100;
    private static final int MAX_WORLDVIEW_COUNT = 200;
    private static final int MAX_CONTEXT_CHARACTERS = 50_000;

    private Novel novel;

    @BeforeEach
    void setUp() {
        novelAiContextService = new NovelAiContextService(characterRepository, worldSettingRepository, episodeSummaryRepository);
        ReflectionTestUtils.setField(novelAiContextService, "chatRecentEpisodeCount", CHAT_RECENT_EPISODE_COUNT);
        ReflectionTestUtils.setField(novelAiContextService, "conflictRecentEpisodeCount", CONFLICT_RECENT_EPISODE_COUNT);
        ReflectionTestUtils.setField(novelAiContextService, "maxCharacterCount", MAX_CHARACTER_COUNT);
        ReflectionTestUtils.setField(novelAiContextService, "maxWorldviewCount", MAX_WORLDVIEW_COUNT);
        ReflectionTestUtils.setField(novelAiContextService, "maxContextCharacters", MAX_CONTEXT_CHARACTERS);

        novel = Novel.builder().title("테스트 작품").genre("판타지").description("설명").build();
        ReflectionTestUtils.setField(novel, "id", 1L);
        // characterRepository/worldSettingRepository/episodeSummaryRepository는 각 테스트에서 필요한 경우에만
        // 스텁한다 — Mockito는 스텁되지 않은 List 반환 메서드에 대해 빈 리스트를 기본 반환하므로 충분하다.
    }

    private Episode episode(int number, String title) {
        Episode episode = Episode.builder().novel(novel).title(title).episodeNumber(number).content("본문").build();
        ReflectionTestUtils.setField(episode, "id", (long) number);
        return episode;
    }

    private EpisodeSummary summary(Episode episode, String summary) {
        return EpisodeSummary.builder().episode(episode).summary(summary).build();
    }

    private Character character(String name, boolean favorite, LocalDateTime updatedAt) {
        Character character = Character.builder().novel(novel).name(name).role("주인공").build();
        character.updateFavorite(favorite);
        ReflectionTestUtils.setField(character, "updatedAt", updatedAt);
        return character;
    }

    @Test
    void 채팅용_컨텍스트는_설정된_개수만큼_최근_회차요약만_조회한다() {
        List<EpisodeSummary> recent = List.of(
                summary(episode(10, "10화"), "10화 요약"),
                summary(episode(9, "9화"), "9화 요약"),
                summary(episode(8, "8화"), "8화 요약")
        );
        given(episodeSummaryRepository.findRecentSummariesByNovel(eq(novel), any(Pageable.class))).willReturn(recent);

        NovelAiContext context = novelAiContextService.buildForChat(novel);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(episodeSummaryRepository).findRecentSummariesByNovel(eq(novel), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(CHAT_RECENT_EPISODE_COUNT);
        verify(episodeSummaryRepository, never()).findRecentSummariesBeforeEpisode(any(), anyInt(), any());

        // 프롬프트용으로는 오래된 회차 → 최신 회차 순으로 재정렬되어야 한다
        assertThat(context.episodeSummaries()).extracting(EpisodeSummaryContext::episodeNumber)
                .containsExactly(8, 9, 10);
    }

    @Test
    void 충돌감지용_컨텍스트는_대상회차_이전의_최근_N개만_조회한다() {
        given(episodeSummaryRepository.findRecentSummariesBeforeEpisode(eq(novel), eq(100), any(Pageable.class)))
                .willReturn(List.of(summary(episode(99, "99화"), "99화 요약")));

        novelAiContextService.buildForConflictDetection(novel, 100);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(episodeSummaryRepository).findRecentSummariesBeforeEpisode(eq(novel), eq(100), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(CONFLICT_RECENT_EPISODE_COUNT);
        verify(episodeSummaryRepository, never()).findRecentSummariesByNovel(any(), any());
    }

    @Test
    void 등장인물이_최대개수를_초과하면_즐겨찾기와_최근수정_순으로_잘린다() {
        ReflectionTestUtils.setField(novelAiContextService, "maxCharacterCount", 2);
        given(episodeSummaryRepository.findRecentSummariesByNovel(any(), any())).willReturn(List.of());

        Character favorite = character("즐겨찾기캐릭터", true, LocalDateTime.now().minusDays(10));
        Character recentlyUpdated = character("최근수정캐릭터", false, LocalDateTime.now());
        Character oldNonFavorite = character("오래된캐릭터", false, LocalDateTime.now().minusDays(30));
        given(characterRepository.findAllByNovelOrderByIsFavoriteDescNameAsc(novel))
                .willReturn(List.of(favorite, recentlyUpdated, oldNonFavorite));

        NovelAiContext context = novelAiContextService.buildForChat(novel);

        assertThat(context.characters()).extracting(CharacterContext::name)
                .containsExactly("즐겨찾기캐릭터", "최근수정캐릭터");
    }

    @Test
    void 전체_참고데이터가_문자예산을_초과하면_낮은우선순위_항목부터_제외된다() {
        // 인물 1명조차 다 담기 어려울 만큼 예산을 극단적으로 줄인다
        ReflectionTestUtils.setField(novelAiContextService, "maxContextCharacters", 5);
        given(episodeSummaryRepository.findRecentSummariesByNovel(any(), any())).willReturn(List.of());

        Character c1 = character("가나다라마바사아자차카", false, LocalDateTime.now());
        given(characterRepository.findAllByNovelOrderByIsFavoriteDescNameAsc(novel)).willReturn(List.of(c1));

        NovelAiContext context = novelAiContextService.buildForChat(novel);

        assertThat(context.characters()).isEmpty();
    }

    @Test
    void 조회시_전달받은_작품으로만_리포지토리를_조회한다() {
        given(episodeSummaryRepository.findRecentSummariesByNovel(any(), any())).willReturn(List.of());

        novelAiContextService.buildForChat(novel);

        verify(characterRepository).findAllByNovelOrderByIsFavoriteDescNameAsc(novel);
        verify(worldSettingRepository).findAllByNovelOrderByCategoryAscIsFavoriteDescTitleAsc(novel);
    }
}
