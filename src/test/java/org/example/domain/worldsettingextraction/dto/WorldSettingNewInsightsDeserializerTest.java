package org.example.domain.worldsettingextraction.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// OpenAI가 newInsights를 프롬프트가 요구한 {"content":[...]} 객체 형태 대신
// 배열([] 또는 ["..."])로 반환해 파싱이 실패했던 문제(WorldSettingExtractionService.parseJson)에 대한
// 회귀 테스트. 실제 서비스 흐름과 동일하게 WorldSettingCandidateDto를 통째로 역직렬화해서 검증한다.
class WorldSettingNewInsightsDeserializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 객체_형태는_content로_정상_파싱된다() throws Exception {
        String json = candidateJson("\"newInsights\":{\"content\":[\"새 정보1\",\"새 정보2\"]}");

        WorldSettingCandidateDto dto = objectMapper.readValue(json, WorldSettingCandidateDto.class);

        assertThat(dto.getNewInsights()).isNotNull();
        assertThat(dto.getNewInsights().getContent()).containsExactly("새 정보1", "새 정보2");
    }

    @Test
    void 빈_배열로_와도_빈_content로_정상_파싱된다() throws Exception {
        String json = candidateJson("\"newInsights\":[]");

        WorldSettingCandidateDto dto = objectMapper.readValue(json, WorldSettingCandidateDto.class);

        assertThat(dto.getNewInsights()).isNotNull();
        assertThat(dto.getNewInsights().getContent()).isEmpty();
    }

    @Test
    void 문자열_배열로_와도_content로_정상_파싱된다() throws Exception {
        String json = candidateJson("\"newInsights\":[\"새 정보1\"]");

        WorldSettingCandidateDto dto = objectMapper.readValue(json, WorldSettingCandidateDto.class);

        assertThat(dto.getNewInsights()).isNotNull();
        assertThat(dto.getNewInsights().getContent()).containsExactly("새 정보1");
    }

    @Test
    void null이면_newInsights도_null이다() throws Exception {
        String json = candidateJson("\"newInsights\":null");

        WorldSettingCandidateDto dto = objectMapper.readValue(json, WorldSettingCandidateDto.class);

        assertThat(dto.getNewInsights()).isNull();
    }

    private String candidateJson(String newInsightsField) {
        return "{\"category\":\"ETC\",\"title\":\"제목\",\"content\":\"내용\","
                + "\"evidence\":\"근거\",\"isExistingSetting\":true,\"matchedWorldSettingId\":1,"
                + newInsightsField + "}";
    }
}
