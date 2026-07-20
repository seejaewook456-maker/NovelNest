package org.example.domain.worldsettingextraction.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// OpenAI가 "새로운 정보 없음"을 표현할 때 프롬프트가 요구한 {"content":[...]} 객체 대신
// 배열([] 또는 ["..."])을 newInsights 자리에 그대로 반환하는 경우가 있어(비결정적 응답),
// 객체/배열/null 형태를 모두 List<String>으로 정규화해서 받아들인다.
public class WorldSettingNewInsightsDeserializer extends JsonDeserializer<WorldSettingNewInsightsDto> {

    @Override
    public WorldSettingNewInsightsDto deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node == null || node.isNull()) {
            return null;
        }

        // 배열이 그대로 온 경우 그 배열 자체를 content로, 객체로 온 경우 "content" 필드를 사용
        JsonNode contentNode = node.isArray() ? node : node.get("content");

        List<String> content = new ArrayList<>();
        if (contentNode != null && contentNode.isArray()) {
            for (JsonNode item : contentNode) {
                if (item.isTextual()) {
                    content.add(item.asText());
                }
            }
        }
        return new WorldSettingNewInsightsDto(content);
    }
}
