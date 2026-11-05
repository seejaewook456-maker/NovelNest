package org.example.global.ai.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiJsonRepairTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 객체를_닫은_뒤_남는_중괄호를_제거한다() throws Exception {
        // 실제 오류와 같은 형태: "Unexpected close marker '}': expected ']'"
        String broken = "[{\"title\":\"A\",\"newInsights\":{\"content\":[\"x\"]}}},{\"title\":\"B\"}]";
        assertThatThrownBy(() -> objectMapper.readTree(broken))
                .hasMessageContaining("Unexpected close marker '}'");

        String repaired = AiJsonRepair.balanceBrackets(broken);
        assertThat(repaired).isEqualTo("[{\"title\":\"A\",\"newInsights\":{\"content\":[\"x\"]}},{\"title\":\"B\"}]");
        assertThat(objectMapper.readTree(repaired).size()).isEqualTo(2);
    }

    @Test
    void 덜_닫힌_안쪽_배열과_끝의_괄호를_보충한다() {
        assertThat(AiJsonRepair.balanceBrackets("[{\"a\":[\"x\"}")).isEqualTo("[{\"a\":[\"x\"]}]");
    }

    @Test
    void 문자열_안의_괄호와_이스케이프된_따옴표는_건드리지_않는다() {
        String json = "[{\"content\":\"그는 \\\"}]\\\"라고 외쳤다 {}\"}]";
        assertThat(AiJsonRepair.balanceBrackets(json)).isEqualTo(json);
    }
}
