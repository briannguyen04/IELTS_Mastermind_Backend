package com.ieltsmastermind.user.management.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class HomePageResponseDto {
    private String id;
    private String email;
    private String fullName;
    private String role;
    private String avatarUrl;
}
