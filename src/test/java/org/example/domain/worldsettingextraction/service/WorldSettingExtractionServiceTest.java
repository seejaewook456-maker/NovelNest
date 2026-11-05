package org.example.domain.worldsettingextraction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.domain.airatelimit.service.AiRateLimitService;
import org.example.domain.aiusage.service.AiUsageService;
import org.example.domain.episode.entity.Episode;
import org.example.domain.episode.repository.EpisodeRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.domain.worldsetting.entity.WorldSetting;
import org.example.domain.worldsetting.entity.WorldSettingCategory;
import org.example.domain.worldsetting.repository.WorldSettingRepository;
import org.example.domain.worldsettingextraction.dto.WorldSettingCandidateDto;
import org.example.domain.worldsettingextraction.dto.WorldSettingExtractionResponseDto;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// 세계관 설정 추출 결과에서 "변경사항 없는 기존 설정"이 제외되는지 검증한다.
@ExtendWith(MockitoExtension.class)
class WorldSettingExtractionServiceTest {

    @Mock private EpisodeRepository episodeRepository;
    @Mock private WorldSettingRepository worldSettingRepository;
    @Mock private UserRepository userRepository;
    @Mock private OpenAiService openAiService;
    @Mock private AiUsageService aiUsageService;
    @Mock private AiRateLimitService aiRateLimitService;

    private WorldSettingExtractionService service;

    private static final String EMAIL = "writer@example.com";
    private static final String EXISTING_CONTENT = "마나를 소모해 마법을 사용한다. 마나는 수면으로 회복된다.";
    private WorldSetting magic;

    @BeforeEach
    void setUp() {
        service = new WorldSettingExtractionService(episodeRepository, worldSettingRepository, userRepository,
                openAiService, new ObjectMapper(), aiUsageService, aiRateLimitService);

        User owner = User.builder().email(EMAIL).password("encoded").nickname("작가").provider(Provider.LOCAL).build();
        ReflectionTestUtils.setField(owner, "id", 1L);
        Novel novel = Novel.builder().user(owner).title("작품").genre("판타지").description("설명").build();
        ReflectionTestUtils.setField(novel, "id", 10L);
        Episode episode = Episode.builder().novel(novel).title("제목").episodeNumber(3).content("본문").build();
        ReflectionTestUtils.setField(episode, "id", 100L);

        magic = WorldSetting.builder().novel(novel).category(WorldSettingCategory.MAGIC)
                .title("마나 체계").content(EXISTING_CONTENT).build();
        ReflectionTestUtils.setField(magic, "id", 7L);

        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.of(owner));
        given(episodeRepository.findById(100L)).willReturn(Optional.of(episode));
        given(worldSettingRepository.findAllByNovelOrderByCategoryAscIsFavoriteDescTitleAsc(novel))
                .willReturn(List.of(magic));
    }

    private WorldSettingExtractionResponseDto extractWith(String aiJson) {
        given(openAiService.generateText(anyString(), anyString())).willReturn(aiJson);
        return service.extractWorldSettings(EMAIL, 100L);
    }

    @Test
    void AI가_중괄호를_하나_더_닫아도_복구해서_파싱한다() {
        WorldSettingExtractionResponseDto res = extractWith(
                "[{\"category\":\"PLACE\",\"title\":\"북부 요새\",\"content\":\"국경의 요새\"," +
                "\"isExistingSetting\":false,\"matchedWorldSettingId\":null,\"newInsights\":null}}," +
                "{\"category\":\"ITEM\",\"title\":\"봉인검\",\"content\":\"봉인된 검\"," +
                "\"isExistingSetting\":false,\"matchedWorldSettingId\":null}]");

        assertThat(res.getTotalCount()).isEqualTo(2);
        assertThat(res.getCandidates()).extracting(WorldSettingCandidateDto::getTitle)
                .containsExactly("북부 요새", "봉인검");
    }

    @Test
    void 근거_대사에_잘못된_이스케이프가_있어도_파싱한다() {
        WorldSettingExtractionResponseDto res = extractWith(
                "[{\"category\":\"PLACE\",\"title\":\"북부 요새\",\"content\":\"국경의 요새\"," +
                "\"evidence\":\"그는 \\'요새는 무너지지 않는다\\'고 말했다.\"," +
                "\"isExistingSetting\":false,\"matchedWorldSettingId\":null}]");

        assertThat(res.getCandidates().get(0).getEvidence()).isEqualTo("그는 '요새는 무너지지 않는다'고 말했다.");
    }

    @Test
    void 신규_설정은_그대로_포함된다() {
        WorldSettingExtractionResponseDto res = extractWith(
                "[{\"category\":\"PLACE\",\"title\":\"북부 요새\",\"content\":\"국경의 요새\"," +
                "\"isExistingSetting\":false,\"matchedWorldSettingId\":null}]");

        assertThat(res.getTotalCount()).isEqualTo(1);
        assertThat(res.getCandidates().get(0).getTitle()).isEqualTo("북부 요새");
    }

    @Test
    void AI_통합_설명이_검증을_통과하면_그대로_채택되고_제목과_분류는_DB값을_유지한다() {
        String integrated = "마나를 소모해 마법을 사용한다. 마나는 수면으로 회복되며, 회복 속도는 시전자의 숙련도에 비례한다.";
        WorldSettingExtractionResponseDto res = extractWith(
                "[{\"category\":\"RULE\",\"title\":\"AI가 바꾼 제목\",\"content\":\"" + integrated + "\"," +
                "\"isExistingSetting\":true,\"matchedWorldSettingId\":7," +
                "\"newInsights\":{\"content\":[\"회복 속도는 숙련도에 비례한다\"]}," +
                "\"newContent\":\"회복 속도는 시전자의 숙련도에 비례한다.\"}]");

        WorldSettingCandidateDto c = res.getCandidates().get(0);
        assertThat(c.getContent()).isEqualTo(integrated);
        assertThat(c.getTitle()).isEqualTo("마나 체계");
        assertThat(c.getCategory()).isEqualTo(WorldSettingCategory.MAGIC);
        verify(worldSettingRepository, never()).save(any());
    }

    @Test
    void 기존_설명의_중복을_통합한_간결한_재구성은_채택된다() {
        // 요청서 2-2 예시: 같은 의미가 반복된 기존 설명을 한 문장으로 정리 + 새 정보(중개인) 통합
        ReflectionTestUtils.setField(magic, "content",
                "기억 시장에서는 공식 거래와 비공식 거래가 이루어진다. 비공식 거래는 공식 기록에 남지 않는다. " +
                "북쪽 골목에서는 비공식적인 기억 거래가 이루어진다. 이러한 비공식 거래는 기록이 남지 않아 추적하기 어렵다.");
        String restructured = "기억 시장에서는 공식 거래와 비공식 거래가 이루어지며, 특히 북쪽 골목의 비공식 거래는 " +
                "기록이 남지 않아 유통 경로를 추적하기 어렵다. 비공식 거래는 전문 중개인이 연결한다.";
        WorldSettingExtractionResponseDto res = extractWith(
                "[{\"category\":\"MAGIC\",\"title\":\"마나 체계\",\"content\":\"" + restructured + "\"," +
                "\"isExistingSetting\":true,\"matchedWorldSettingId\":7," +
                "\"newInsights\":{\"content\":[\"비공식 거래는 전문 중개인이 연결한다\"]}," +
                "\"newContent\":\"비공식 거래는 전문 중개인이 연결한다.\"}]");

        assertThat(res.getCandidates().get(0).getContent()).isEqualTo(restructured);
    }

    @Test
    void 새_정보를_관련_문장에_녹인_재구성은_채택된다() {
        // 요청서 2-3 예시
        ReflectionTestUtils.setField(magic, "content",
                "중앙 기억 거래소는 공식적인 기억 거래를 관리하는 시설이다. 거래 과정에서는 신분 확인과 서류 작성이 이루어진다.");
        String restructured = "중앙 기억 거래소는 신분 확인과 서류 작성을 거쳐 공식적인 기억 거래를 관리하는 시설이다. " +
                "거래 기록은 중앙 거리 끝에 위치한 크로아 기억 거래 기록 관리소에서 보관한다.";
        WorldSettingExtractionResponseDto res = extractWith(
                "[{\"category\":\"MAGIC\",\"title\":\"마나 체계\",\"content\":\"" + restructured + "\"," +
                "\"isExistingSetting\":true,\"matchedWorldSettingId\":7," +
                "\"newInsights\":{\"content\":[\"거래 기록은 크로아 기억 거래 기록 관리소에서 보관한다\"]}," +
                "\"newContent\":\"거래 기록은 중앙 거리 끝의 크로아 기억 거래 기록 관리소에서 보관한다.\"}]");

        assertThat(res.getCandidates().get(0).getContent()).isEqualTo(restructured);
    }

    @Test
    void 재구성에서_수치나_고유명사가_빠지면_기존_설명을_보존한_채_병합한다() {
        String existing = "'크로아 관리소'는 거래 기록을 30년간 보관한다. 기록 열람은 관리소장의 허가가 필요하다.";
        ReflectionTestUtils.setField(magic, "content", existing);
        // 수치(30년)와 고유명사('크로아 관리소')를 빠뜨린 요약
        WorldSettingExtractionResponseDto res = extractWith(
                "[{\"category\":\"MAGIC\",\"title\":\"마나 체계\"," +
                "\"content\":\"관리소는 거래 기록을 장기간 보관하며, 기록 열람은 관리소장의 허가가 필요하다. 위조 기록은 소각된다.\"," +
                "\"isExistingSetting\":true,\"matchedWorldSettingId\":7," +
                "\"newInsights\":{\"content\":[\"위조 기록은 소각된다\"]},\"newContent\":\"위조 기록은 소각된다.\"}]");

        assertThat(res.getCandidates().get(0).getContent()).isEqualTo(existing + "\n\n위조 기록은 소각된다.");
    }

    @Test
    void 재구성의_문장앞_회차표현만_제거되면_재구성본이_채택된다() {
        WorldSettingExtractionResponseDto res = extractWith(
                "[{\"category\":\"MAGIC\",\"title\":\"마나 체계\"," +
                "\"content\":\"마나를 소모해 마법을 사용하며, 마나는 수면으로 회복된다. 이번 회차에서는 마나가 고갈되면 기절한다.\"," +
                "\"isExistingSetting\":true,\"matchedWorldSettingId\":7," +
                "\"newInsights\":{\"content\":[\"마나가 고갈되면 기절한다\"]},\"newContent\":\"마나가 고갈되면 기절한다.\"}]");

        assertThat(res.getCandidates().get(0).getContent())
                .isEqualTo("마나를 소모해 마법을 사용하며, 마나는 수면으로 회복된다. 마나가 고갈되면 기절한다.");
    }

    @Test
    void 통합_설명에_회차_중심_표현이_있으면_기존_방식으로_병합하고_문장앞_회차표현을_제거한다() {
        WorldSettingExtractionResponseDto res = extractWith(
                "[{\"category\":\"MAGIC\",\"title\":\"마나 체계\"," +
                "\"content\":\"" + EXISTING_CONTENT + " 이번 회차에서는 마나 과다 사용 시 기절한다는 사실이 드러났다.\"," +
                "\"isExistingSetting\":true,\"matchedWorldSettingId\":7," +
                "\"newInsights\":{\"content\":[\"마나 과다 사용 시 기절한다\"]}," +
                "\"newContent\":\"이번 회차에서는 마나를 과다하게 사용하면 기절한다.\"}]");

        String content = res.getCandidates().get(0).getContent();
        assertThat(content).isEqualTo(EXISTING_CONTENT + "\n\n마나를 과다하게 사용하면 기절한다.");
        assertThat(content).doesNotContain("회차");
    }

    @Test
    void 통합_설명이_기존_내용을_누락하면_기존_설명을_보존한_채_병합한다() {
        // AI가 기존 설명을 요약·삭제하고 새 정보만 남긴 경우
        WorldSettingExtractionResponseDto res = extractWith(
                "[{\"category\":\"MAGIC\",\"title\":\"마나 체계\",\"content\":\"마나 과다 사용 시 기절한다.\"," +
                "\"isExistingSetting\":true,\"matchedWorldSettingId\":7," +
                "\"newInsights\":{\"content\":[\"마나 과다 사용 시 기절한다\"]},\"newContent\":null}]");

        // newContent가 없으면 newInsights 문장으로 예비 병합
        assertThat(res.getCandidates().get(0).getContent())
                .isEqualTo(EXISTING_CONTENT + "\n\n마나 과다 사용 시 기절한다");
    }

    @Test
    void 반복_보강된_설명도_통합본의_문단_구분이_정리된다() {
        // 여러 회차에 걸쳐 보강된 기존 설명 + 3줄 이상 개행이 섞인 통합본
        ReflectionTestUtils.setField(magic, "content", EXISTING_CONTENT + "\n\n고위 마법사는 마나를 저장할 수 있다.");
        String integrated = EXISTING_CONTENT + " 마나가 고갈되면 기절한다.\n\n\n\n고위 마법사는 마나를 저장할 수 있다.";
        WorldSettingExtractionResponseDto res = extractWith(
                "[{\"category\":\"MAGIC\",\"title\":\"마나 체계\",\"content\":\"" + integrated.replace("\n", "\\n") + "\"," +
                "\"isExistingSetting\":true,\"matchedWorldSettingId\":7," +
                "\"newInsights\":{\"content\":[\"마나가 고갈되면 기절한다\"]},\"newContent\":\"마나가 고갈되면 기절한다.\"}]");

        assertThat(res.getCandidates().get(0).getContent()).isEqualTo(
                EXISTING_CONTENT + " 마나가 고갈되면 기절한다.\n\n고위 마법사는 마나를 저장할 수 있다.");
    }

    @Test
    void 신규_설정의_문장앞_회차_표현은_제거된다() {
        WorldSettingExtractionResponseDto res = extractWith(
                "[{\"category\":\"PLACE\",\"title\":\"기록 관리소\"," +
                "\"content\":\"이번 회차에서는 중앙 거리 끝에 기록 관리소가 있다. 이번 회차에서 관리소는 밤에 닫는다.\"," +
                "\"isExistingSetting\":false,\"matchedWorldSettingId\":null}]");

        assertThat(res.getCandidates().get(0).getContent())
                .isEqualTo("중앙 거리 끝에 기록 관리소가 있다. 관리소는 밤에 닫는다.");
    }

    @Test
    void 기존_설정에_새로운_정보가_있으면_포함되고_기존_내용_뒤에_병합된다() {
        WorldSettingExtractionResponseDto res = extractWith(
                "[{\"category\":\"MAGIC\",\"title\":\"마나 체계\",\"content\":\"무시됨\"," +
                "\"isExistingSetting\":true,\"matchedWorldSettingId\":7," +
                "\"newInsights\":{\"content\":[\"마나 과다 사용 시 기절한다\"]}," +
                "\"newContent\":\"마나를 과다하게 사용하면 기절한다.\"}]");

        assertThat(res.getTotalCount()).isEqualTo(1);
        WorldSettingCandidateDto c = res.getCandidates().get(0);
        assertThat(c.getExistingWorldSetting()).isNotNull();
        assertThat(c.getContent()).isEqualTo(EXISTING_CONTENT + "\n\n마나를 과다하게 사용하면 기절한다.");
    }

    @Test
    void 기존_설정이_단순히_다시_언급되면_제외된다() {
        WorldSettingExtractionResponseDto res = extractWith(
                "[{\"category\":\"MAGIC\",\"title\":\"마나 체계\",\"content\":\"" + EXISTING_CONTENT + "\"," +
                "\"isExistingSetting\":true,\"matchedWorldSettingId\":7,\"newInsights\":[],\"newContent\":null}]");

        assertThat(res.getCandidates()).isEmpty();
    }

    @Test
    void 기존_내용에_이미_포함된_정보만_반복되면_제외된다() {
        WorldSettingExtractionResponseDto res = extractWith(
                "[{\"category\":\"MAGIC\",\"title\":\"마나 체계\",\"content\":\"x\"," +
                "\"isExistingSetting\":true,\"matchedWorldSettingId\":7," +
                "\"newInsights\":{\"content\":[\"마나는 수면으로 회복된다\"]}," +
                "\"newContent\":\"마나는 수면으로 회복된다.\"}]");

        assertThat(res.getCandidates()).isEmpty();
    }

    @Test
    void 신규_설정과_변경없는_기존_설정이_섞이면_신규만_남고_DB_쓰기는_없다() {
        WorldSettingExtractionResponseDto res = extractWith(
                "[{\"category\":\"MAGIC\",\"title\":\"마나 체계\",\"content\":\"x\"," +
                "\"isExistingSetting\":true,\"matchedWorldSettingId\":7,\"newInsights\":null}," +
                "{\"category\":\"PLACE\",\"title\":\"북부 요새\",\"content\":\"국경의 요새\"," +
                "\"isExistingSetting\":false,\"matchedWorldSettingId\":null}]");

        assertThat(res.getTotalCount()).isEqualTo(1);
        assertThat(res.getCandidates().get(0).getTitle()).isEqualTo("북부 요새");
        verify(worldSettingRepository, never()).save(any());
        assertThat(magic.getContent()).isEqualTo(EXISTING_CONTENT);
    }
}
