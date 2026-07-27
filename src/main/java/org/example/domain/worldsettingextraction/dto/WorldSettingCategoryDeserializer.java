package org.example.domain.worldsettingextraction.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.worldsetting.entity.WorldSettingCategory;

import java.io.IOException;

// OpenAI가 프롬프트로 지정한 카테고리(COUNTRY/RACE/MAGIC/ORGANIZATION/PLACE/EVENT/ITEM/RULE/ETC) 외의
// 값(예: "SETTING")을 비결정적으로 반환하는 경우가 있어, 매칭되지 않는 값은 예외 대신 ETC로 대체한다.
@Slf4j
public class WorldSettingCategoryDeserializer extends JsonDeserializer<WorldSettingCategory> {

    @Override
    public WorldSettingCategory deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null || value.isBlank()) {
            return WorldSettingCategory.ETC;
        }

        try {
            return WorldSettingCategory.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 WorldSettingCategory 값 수신: {} → ETC로 대체", value);
            return WorldSettingCategory.ETC;
        }
    }
}
