package com.ieltsmastermind.authentication.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserRegisterResponseDto {
    private String email;
    private String firstname;
    private String lastname;
}