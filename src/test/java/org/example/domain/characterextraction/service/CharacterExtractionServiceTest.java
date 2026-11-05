package org.example.domain.characterextraction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.domain.airatelimit.service.AiRateLimitService;
import org.example.domain.aiusage.service.AiUsageService;
import org.example.domain.character.entity.Character;
import org.example.domain.character.repository.CharacterRepository;
import org.example.domain.characterextraction.dto.CharacterCandidateDto;
import org.example.domain.characterextraction.dto.CharacterExtractionResponseDto;
import org.example.domain.episode.entity.Episode;
import org.example.domain.episode.repository.EpisodeRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.global.ai.service.OpenAiService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// 등장인물 추출 결과에서 "변경사항 없는 기존 인물"이 제외되는지 검증한다.
// AI 응답은 목(mock)으로 고정해, 서버 측 2차 검증 로직만 확인한다.
@ExtendWith(MockitoExtension.class)
class CharacterExtractionServiceTest {

    @Mock private EpisodeRepository episodeRepository;
    @Mock private CharacterRepository characterRepository;
    @Mock private UserRepository userRepository;
    @Mock private OpenAiService openAiService;
    @Mock private AiUsageService aiUsageService;
    @Mock private AiRateLimitService aiRateLimitService;

    private CharacterExtractionService service;

    private static final String EMAIL = "writer@example.com";
    private Novel novel;
    private Character hero;

    @BeforeEach
    void setUp() {
        service = new CharacterExtractionService(episodeRepository, characterRepository, userRepository,
                openAiService, new ObjectMapper(), aiUsageService, aiRateLimitService);

        User owner = User.builder().email(EMAIL).password("encoded").nickname("작가").provider(Provider.LOCAL).build();
        ReflectionTestUtils.setField(owner, "id", 1L);
        novel = Novel.builder().user(owner).title("작품").genre("판타지").description("설명").build();
        ReflectionTestUtils.setField(novel, "id", 10L);
        Episode episode = Episode.builder().novel(novel).title("제목").episodeNumber(3).content("본문").build();
        ReflectionTestUtils.setField(episode, "id", 100L);

        hero = Character.builder().novel(novel).name("이안").role("주인공").age(20)
                .personality("냉정함, 침착함").speechStyle("존댓말").description("검술에 능하다.").build();
        ReflectionTestUtils.setField(hero, "id", 5L);

        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.of(owner));
        given(episodeRepository.findById(100L)).willReturn(Optional.of(episode));
        given(characterRepository.findAllByNovelOrderByIsFavoriteDescNameAsc(novel)).willReturn(List.of(hero));
    }

    private CharacterExtractionResponseDto extractWith(String aiJson) {
        given(openAiService.generateText(anyString(), anyString())).willReturn(aiJson);
        return service.extractCharacters(EMAIL, 100L);
    }

    @Test
    void 근거_대사에_잘못된_이스케이프가_있어도_파싱한다() {
        // 실제 오류 재현: evidence 안의 \' (JSON에서 허용되지 않는 이스케이프)
        CharacterExtractionResponseDto res = extractWith(
                "[{\"name\":\"레아\",\"evidence\":\"레아는 \\'여기서 멈춰\\'라고 외쳤다.\"," +
                "\"isExistingCharacter\":false,\"matchedCharacterId\":null}]");

        assertThat(res.getCandidates().get(0).getEvidence()).isEqualTo("레아는 '여기서 멈춰'라고 외쳤다.");
    }

    @Test
    void 신규_인물은_그대로_포함된다() {
        CharacterExtractionResponseDto res = extractWith(
                "[{\"name\":\"레아\",\"isExistingCharacter\":false,\"matchedCharacterId\":null,\"newInsights\":null}]");

        assertThat(res.getTotalCount()).isEqualTo(1);
        assertThat(res.getCandidates().get(0).getName()).isEqualTo("레아");
    }

    @Test
    void 기존_인물의_새로운_정보가_있으면_포함된다() {
        CharacterExtractionResponseDto res = extractWith(
                "[{\"name\":\"이안\",\"isExistingCharacter\":true,\"matchedCharacterId\":5," +
                "\"newInsights\":{\"personality\":[\"다정함\"],\"description\":[\"왼쪽 뺨에 흉터가 있다\"]}}]");

        assertThat(res.getTotalCount()).isEqualTo(1);
        CharacterCandidateDto c = res.getCandidates().get(0);
        assertThat(c.getExistingCharacter()).isNotNull();
        assertThat(c.getNewInsights().getPersonality()).containsExactly("다정함");
        assertThat(c.getNewInsights().getDescription()).containsExactly("왼쪽 뺨에 흉터가 있다");
    }

    @Test
    void 기존_인물이_새_정보_없이_재등장하면_제외된다() {
        CharacterExtractionResponseDto res = extractWith(
                "[{\"name\":\"이안\",\"isExistingCharacter\":true,\"matchedCharacterId\":5,\"newInsights\":null}," +
                "{\"name\":\"이안\",\"isExistingCharacter\":true,\"matchedCharacterId\":5," +
                "\"newInsights\":{\"personality\":[],\"speechStyle\":[]}}]");

        assertThat(res.getTotalCount()).isZero();
        assertThat(res.getCandidates()).isEmpty();
    }

    @Test
    void 기존_정보에_이미_포함된_내용만_반복되면_제외된다() {
        // 공백·문장부호만 다르거나 기존 설명에 그대로 포함된 내용, 기존과 같은 역할/나이
        CharacterExtractionResponseDto res = extractWith(
                "[{\"name\":\"이안\",\"isExistingCharacter\":true,\"matchedCharacterId\":5," +
                "\"newInsights\":{\"personality\":[\"냉정함.\",\" 침착함 \"],\"speechStyle\":[\"존댓말\"]," +
                "\"description\":[\"검술에 능하다\"],\"role\":\"주인공\",\"age\":20}}]");

        assertThat(res.getCandidates()).isEmpty();
    }

    @Test
    void 역할이나_나이가_실제로_바뀌면_포함된다() {
        CharacterExtractionResponseDto res = extractWith(
                "[{\"name\":\"이안\",\"isExistingCharacter\":true,\"matchedCharacterId\":5," +
                "\"newInsights\":{\"personality\":[\"냉정함\"],\"role\":\"주인공\",\"age\":21}}]");

        assertThat(res.getTotalCount()).isEqualTo(1);
        CharacterCandidateDto c = res.getCandidates().get(0);
        assertThat(c.getNewInsights().getAge()).isEqualTo(21);
        assertThat(c.getNewInsights().getRole()).isNull();          // 기존과 같은 역할은 제거
        assertThat(c.getNewInsights().getPersonality()).isEmpty(); // 기존에 있던 성격은 제거
    }

    @Test
    void 신규_인물과_변경없는_기존_인물이_섞이면_신규만_남는다() {
        CharacterExtractionResponseDto res = extractWith(
                "[{\"name\":\"이안\",\"isExistingCharacter\":true,\"matchedCharacterId\":5,\"newInsights\":null}," +
                "{\"name\":\"레아\",\"isExistingCharacter\":false,\"matchedCharacterId\":null}]");

        assertThat(res.getTotalCount()).isEqualTo(1);
        assertThat(res.getCandidates().get(0).getName()).isEqualTo("레아");
    }

    @Test
    void AI_응답_파싱_실패는_변경사항_없음이_아니라_오류로_처리된다() {
        given(openAiService.generateText(anyString(), anyString())).willReturn("JSON 아님");

        assertThatThrownBy(() -> service.extractCharacters(EMAIL, 100L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 변경사항_필터링_중_DB_쓰기는_발생하지_않는다() {
        extractWith("[{\"name\":\"이안\",\"isExistingCharacter\":true,\"matchedCharacterId\":5,\"newInsights\":null}]");

        verify(characterRepository, never()).save(any());
        // 기존 엔티티 값도 그대로 유지된다
        assertThat(hero.getPersonality()).isEqualTo("냉정함, 침착함");
    }
}
