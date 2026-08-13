package org.example.domain.memo.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class MemoFavoriteRequestDto {

    @NotNull
    private final Boolean isFavorite;

    @JsonCreator
    public MemoFavoriteRequestDto(@JsonProperty("isFavorite") Boolean isFavorite) {
        this.isFavorite = isFavorite;
    }
}
