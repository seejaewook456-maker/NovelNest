package org.example.domain.conflictdetection.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.domain.conflictdetection.dto.ConflictDetectionResponseDto;
import org.example.domain.conflictdetection.entity.ConflictDetectionResult;
import org.example.domain.conflictdetection.repository.ConflictDetectionResultRepository;
import org.example.domain.episode.entity.Episode;
import org.example.domain.episode.repository.EpisodeRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.global.ai.context.CharacterContext;
import org.example.global.ai.context.EpisodeSummaryContext;
import org.example.global.ai.context.NovelAiContext;
import org.example.global.ai.context.NovelAiContextService;
import org.example.global.ai.context.WorldSettingContext;
import org.example.global.ai.service.OpenAiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConflictDetectionServiceTest {

    @Mock
    private EpisodeRepository episodeRepository;
    @Mock
    private ConflictDetectionResultRepository conflictDetectionResultRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NovelAiContextService novelAiContextService;
    @Mock
    private OpenAiService openAiService;

    private ConflictDetectionService conflictDetectionService;

    private static final String EMAIL = "writer@example.com";
    private static final int MAX_OUTPUT_TOKENS = 1200;

    private User owner;
    private Novel novel;
    private Episode episode;

    @BeforeEach
    void setUp() {
        conflictDetectionService = new ConflictDetectionService(
                episodeRepository, conflictDetectionResultRepository, userRepository,
                novelAiContextService, openAiService, new ObjectMapper());
        ReflectionTestUtils.setField(conflictDetectionService, "conflictMaxOutputTokens", MAX_OUTPUT_TOKENS);

        owner = User.builder().email(EMAIL).password("encoded").nickname("작가").provider(Provider.LOCAL).build();
        ReflectionTestUtils.setField(owner, "id", 1L);

        novel = Novel.builder().user(owner).title("테스트 작품").genre("판타지").description("설명").build();
        ReflectionTestUtils.setField(novel, "id", 10L);

        episode = Episode.builder().novel(novel).title("100화").episodeNumber(100).content("100화 본문 내용").build();
        ReflectionTestUtils.setField(episode, "id", 100L);
    }

    private NovelAiContext sampleContext() {
        return new NovelAiContext(
                List.of(new CharacterContext("주인공", "주인공", 20, "밝음", "반말", "설명")),
                List.of(new WorldSettingContext("MAGIC", "마법 체계", "마법 내용")),
                List.of(new EpisodeSummaryContext(99, "99화", "99화 요약"))
        );
    }

    @Test
    void AI_호출은_한번만_실행되고_설정된_최대출력토큰이_전달된다() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(owner));
        given(episodeRepository.findById(100L)).willReturn(Optional.of(episode));
        given(novelAiContextService.buildForConflictDetection(novel, episode)).willReturn(sampleContext());
        given(openAiService.generateText(anyString(), anyString(), eq(MAX_OUTPUT_TOKENS))).willReturn("[]");
        given(conflictDetectionResultRepository.findByEpisode(episode)).willReturn(Optional.empty());
        given(conflictDetectionResultRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        conflictDetectionService.detectConflicts(EMAIL, 100L);

        verify(openAiService, times(1)).generateText(anyString(), anyString(), eq(MAX_OUTPUT_TOKENS));
        verify(novelAiContextService).buildForConflictDetection(novel, episode);
    }

    @Test
    void 새회차_본문과_등장인물_세계관_직전요약이_프롬프트에_포함된다() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(owner));
        given(episodeRepository.findById(100L)).willReturn(Optional.of(episode));
        given(novelAiContextService.buildForConflictDetection(novel, episode)).willReturn(sampleContext());
        given(openAiService.generateText(anyString(), anyString(), any())).willReturn("[]");
        given(conflictDetectionResultRepository.findByEpisode(episode)).willReturn(Optional.empty());
        given(conflictDetectionResultRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        conflictDetectionService.detectConflicts(EMAIL, 100L);

        ArgumentCaptor<String> inputCaptor = ArgumentCaptor.forClass(String.class);
        verify(openAiService).generateText(anyString(), inputCaptor.capture(), any());
        String input = inputCaptor.getValue();

        assertThat(input).contains("100화 본문 내용");
        assertThat(input).contains("주인공");
        assertThat(input).contains("마법 체계");
        assertThat(input).contains("99화 요약");
    }

    @Test
    void 다른_사용자의_회차이면_권한_예외가_발생하고_AI를_호출하지_않는다() {
        User other = User.builder().email("other@example.com").password("encoded").nickname("다른사람").provider(Provider.LOCAL).build();
        ReflectionTestUtils.setField(other, "id", 2L);

        given(userRepository.findByEmail("other@example.com")).willReturn(Optional.of(other));
        given(episodeRepository.findById(100L)).willReturn(Optional.of(episode));

        assertThatThrownBy(() -> conflictDetectionService.detectConflicts("other@example.com", 100L))
                .isInstanceOf(SecurityException.class);

        verify(openAiService, never()).generateText(anyString(), anyString(), any());
    }

    @Test
    void 기존_분석결과가_있으면_새로_저장하지_않고_업데이트한다() {
        ConflictDetectionResult existing = ConflictDetectionResult.builder()
                .episode(episode).conflictsJson("[]").conflictCount(0).build();

        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(owner));
        given(episodeRepository.findById(100L)).willReturn(Optional.of(episode));
        given(novelAiContextService.buildForConflictDetection(novel, episode)).willReturn(sampleContext());
        given(openAiService.generateText(anyString(), anyString(), any())).willReturn("[]");
        given(conflictDetectionResultRepository.findByEpisode(episode)).willReturn(Optional.of(existing));

        ConflictDetectionResponseDto response = conflictDetectionService.detectConflicts(EMAIL, 100L);

        verify(conflictDetectionResultRepository, never()).save(any());
        assertThat(response.getConflictCount()).isEqualTo(0);
    }
}
