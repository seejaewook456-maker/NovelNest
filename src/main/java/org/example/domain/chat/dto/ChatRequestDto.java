package org.example.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRequestDto {

    @NotBlank(message = "메시지를 입력해 주세요.")
    @Size(max = 2000, message = "메시지는 2000자 이내로 입력해 주세요.")
    private String message;
}
