package com.ieltsmastermind.authentication.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserVerifyCodeResponseDto {
    private String resetToken;
}
