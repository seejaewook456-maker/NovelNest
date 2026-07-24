package org.example.domain.chat.service;

import org.example.domain.character.repository.CharacterRepository;
import org.example.domain.chat.dto.ChatResponseDto;
import org.example.domain.episode.repository.EpisodeRepository;
import org.example.domain.episodesummary.repository.EpisodeSummaryRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.novel.repository.NovelRepository;
import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.domain.worldsetting.repository.WorldSettingRepository;
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
import org.mockito.InjectMocks;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private NovelRepository novelRepository;
    @Mock
    private CharacterRepository characterRepository;
    @Mock
    private WorldSettingRepository worldSettingRepository;
    @Mock
    private EpisodeSummaryRepository episodeSummaryRepository;
    @Mock
    private EpisodeRepository episodeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NovelAiContextService novelAiContextService;
    @Mock
    private OpenAiService openAiService;

    @InjectMocks
    private ChatService chatService;

    private static final String EMAIL = "writer@example.com";
    private static final int MAX_OUTPUT_TOKENS = 700;

    private User owner;
    private Novel novel;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chatService, "chatMaxOutputTokens", MAX_OUTPUT_TOKENS);

        owner = User.builder().email(EMAIL).password("encoded").nickname("작가").provider(Provider.LOCAL).build();
        ReflectionTestUtils.setField(owner, "id", 1L);

        novel = Novel.builder().user(owner).title("테스트 작품").genre("판타지").description("설명").build();
        ReflectionTestUtils.setField(novel, "id", 10L);
    }

    private NovelAiContext sampleContext() {
        return new NovelAiContext(
                List.of(new CharacterContext("주인공", "주인공", 20, "밝음", "반말", "설명")),
                List.of(new WorldSettingContext("MAGIC", "마법 체계", "마법 내용")),
                List.of(new EpisodeSummaryContext(9, "9화", "9화 요약"))
        );
    }

    @Test
    void AI_호출은_한번만_실행되고_설정된_최대출력토큰이_전달된다() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(owner));
        given(novelRepository.findById(10L)).willReturn(Optional.of(novel));
        given(novelAiContextService.buildForChat(novel)).willReturn(sampleContext());
        given(openAiService.generateText(anyString(), anyString(), eq(MAX_OUTPUT_TOKENS))).willReturn("AI 답변");

        ChatResponseDto response = chatService.chat(EMAIL, 10L, "질문입니다");

        assertThat(response.getAnswer()).isEqualTo("AI 답변");
        verify(openAiService, times(1)).generateText(anyString(), anyString(), eq(MAX_OUTPUT_TOKENS));
    }

    @Test
    void 등장인물_세계관_최근회차요약이_프롬프트에_포함된다() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(owner));
        given(novelRepository.findById(10L)).willReturn(Optional.of(novel));
        given(novelAiContextService.buildForChat(novel)).willReturn(sampleContext());
        given(openAiService.generateText(anyString(), anyString(), any())).willReturn("AI 답변");

        chatService.chat(EMAIL, 10L, "질문입니다");

        ArgumentCaptor<String> inputCaptor = ArgumentCaptor.forClass(String.class);
        verify(openAiService).generateText(anyString(), inputCaptor.capture(), any());
        String input = inputCaptor.getValue();

        assertThat(input).contains("주인공");
        assertThat(input).contains("마법 체계");
        assertThat(input).contains("9화 요약");
        assertThat(input).contains("질문입니다");
    }

    @Test
    void 다른_사용자의_작품이면_권한_예외가_발생하고_AI를_호출하지_않는다() {
        User other = User.builder().email("other@example.com").password("encoded").nickname("다른사람").provider(Provider.LOCAL).build();
        ReflectionTestUtils.setField(other, "id", 2L);

        given(userRepository.findByEmail("other@example.com")).willReturn(Optional.of(other));
        given(novelRepository.findById(10L)).willReturn(Optional.of(novel));

        assertThatThrownBy(() -> chatService.chat("other@example.com", 10L, "질문"))
                .isInstanceOf(SecurityException.class);

        verify(openAiService, times(0)).generateText(anyString(), anyString(), any());
    }
}
