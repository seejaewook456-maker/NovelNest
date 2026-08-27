package org.example.domain.episode.service;

import org.example.domain.conflictdetection.repository.ConflictDetectionResultRepository;
import org.example.domain.episode.dto.EpisodeBriefResponseDto;
import org.example.domain.episode.entity.Episode;
import org.example.domain.episode.repository.EpisodeRepository;
import org.example.domain.episodecharacter.repository.EpisodeCharacterRepository;
import org.example.domain.episodesummary.repository.EpisodeSummaryRepository;
import org.example.domain.episodeworldsetting.repository.EpisodeWorldSettingRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.novel.repository.NovelRepository;
import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

// "이전 회차" 참고 패널을 위해 새로 추가한 EpisodeService.getEpisodeBriefs만 검증한다.
// createEpisode/getEpisode/updateEpisode/deleteEpisode 등 기존 메서드는 이번 작업 범위가 아니므로
// 별도로 다루지 않는다(기존에도 테스트가 없던 영역).
@ExtendWith(MockitoExtension.class)
class EpisodeServiceTest {

    @Mock
    private EpisodeRepository episodeRepository;
    @Mock
    private EpisodeCharacterRepository episodeCharacterRepository;
    @Mock
    private EpisodeWorldSettingRepository episodeWorldSettingRepository;
    @Mock
    private EpisodeSummaryRepository episodeSummaryRepository;
    @Mock
    private ConflictDetectionResultRepository conflictDetectionResultRepository;
    @Mock
    private NovelRepository novelRepository;
    @Mock
    private UserRepository userRepository;

    private EpisodeService episodeService;

    private static final String EMAIL = "writer@example.com";
    private static final String OTHER_EMAIL = "other@example.com";

    private User owner;
    private User other;
    private Novel novel;

    @BeforeEach
    void setUp() {
        episodeService = new EpisodeService(
                episodeRepository,
                episodeCharacterRepository,
                episodeWorldSettingRepository,
                episodeSummaryRepository,
                conflictDetectionResultRepository,
                novelRepository,
                userRepository
        );

        owner = User.builder().email(EMAIL).password("encoded").nickname("작가").provider(Provider.LOCAL).build();
        ReflectionTestUtils.setField(owner, "id", 1L);

        other = User.builder().email(OTHER_EMAIL).password("encoded").nickname("남").provider(Provider.LOCAL).build();
        ReflectionTestUtils.setField(other, "id", 2L);

        novel = Novel.builder().user(owner).title("테스트 작품").genre("판타지").description("설명").build();
        ReflectionTestUtils.setField(novel, "id", 10L);
    }

    private Episode episode(Novel novel, String title, int episodeNumber, String content, Long id) {
        Episode episode = Episode.builder()
                .novel(novel)
                .title(title)
                .episodeNumber(episodeNumber)
                .content(content)
                .build();
        ReflectionTestUtils.setField(episode, "id", id);
        return episode;
    }

    @Test
    void 이전_회차_목록은_본문_없이_번호와_제목만_반환한다() {
        Episode e1 = episode(novel, "1화 제목", 1, "1화 아주 긴 본문...", 100L);
        Episode e2 = episode(novel, "2화 제목", 2, "2화 아주 긴 본문...", 101L);
        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.of(owner));
        given(novelRepository.findById(10L)).willReturn(Optional.of(novel));
        given(episodeRepository.findAllByNovelOrderByEpisodeNumberAsc(novel)).willReturn(List.of(e1, e2));

        List<EpisodeBriefResponseDto> result = episodeService.getEpisodeBriefs(EMAIL, 10L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(EpisodeBriefResponseDto::getEpisodeNumber).containsExactly(1, 2);
        assertThat(result).extracting(EpisodeBriefResponseDto::getTitle).containsExactly("1화 제목", "2화 제목");
    }

    @Test
    void 다른_사용자의_작품_이전_회차_목록에는_접근할_수_없다() {
        Novel otherNovel = Novel.builder().user(other).title("남의 작품").genre("로맨스").description("설명").build();
        ReflectionTestUtils.setField(otherNovel, "id", 20L);

        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.of(owner));
        given(novelRepository.findById(20L)).willReturn(Optional.of(otherNovel));

        assertThatThrownBy(() -> episodeService.getEpisodeBriefs(EMAIL, 20L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("권한이 없습니다");
    }
}
