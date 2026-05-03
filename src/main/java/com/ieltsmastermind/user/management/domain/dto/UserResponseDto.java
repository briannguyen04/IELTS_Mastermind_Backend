package com.ieltsmastermind.user.management.domain.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

@Getter
@Setter
public class UserResponseDto {
    private String userId;
    private String email;
    private String phoneNumber;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private String role;
    private String firstname;
    private String lastname;
    private String gender;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    private String country;
    private String timezone;
    private String avatarUrl;
    private Integer targetBand;
    private Double targetListeningBand;
    private Double targetReadingBand;
    private Double targetWritingBand;
    private Double targetSpeakingBand;
    private LocalDateTime examDate;
}
