package com.ieltsmastermind.user.management.business.service;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.user.management.business.interfaces.UserService;
import com.ieltsmastermind.user.management.domain.dto.UserCreateRequestDto;
import com.ieltsmastermind.user.management.domain.dto.UserResponseDto;
import com.ieltsmastermind.user.management.domain.dto.UserUpdateRequestDto;
import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.persistence.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponseDto create(UserCreateRequestDto request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use: " + request.getEmail());
        }

        if (request.getPhoneNumber() != null &&
                userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Phone already in use: " + request.getPhoneNumber());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        user.setFirstname(request.getFirstname());
        user.setLastname(request.getLastname());
        user.setCountry(request.getCountry());
        user.setTimezone(request.getTimezone());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setTargetBand(request.getTargetBand());
        user.setExamDate(request.getExamDate());

        user.setRole(request.getRole());
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLoginAt(null);

        User saved = userRepository.save(user);

        UserResponseDto responseDto = new UserResponseDto();
        responseDto.setUserId(saved.getUserId());

        return responseDto;
    }

    @Override
    @Transactional
    public List<UserResponseDto> getAll(IncludeSpec includes) {
        List<User> users = userRepository.findAll();
        List<UserResponseDto> result = new ArrayList<>();

        for (User user : users) {
            UserResponseDto dto = new UserResponseDto();
            dto.setUserId(user.getUserId());
            applyIncludes(user, dto, includes);
            result.add(dto);
        }

        return result;
    }

    @Override
    @Transactional
    public UserResponseDto getById(String id, IncludeSpec includes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        UserResponseDto dto = new UserResponseDto();
        dto.setUserId(user.getUserId());   // always include id (recommended)
        applyIncludes(user, dto, includes);

        return dto;
    }

    @Override
    @Transactional
    public UserResponseDto update(String id, UserUpdateRequestDto request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (request.getEmail() != null
                && !request.getEmail().equals(user.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use: " + request.getEmail());
        }

        if (request.getPhoneNumber() != null
                && !request.getPhoneNumber().equals(user.getPhoneNumber())
                && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Phone already in use: " + request.getPhoneNumber());
        }

        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getFirstname() != null) user.setFirstname(request.getFirstname());
        if (request.getLastname() != null) user.setLastname(request.getLastname());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getCountry() != null) user.setCountry(request.getCountry());
        if (request.getTimezone() != null) user.setTimezone(request.getTimezone());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getTargetBand() != null) user.setTargetBand(request.getTargetBand());
        if (request.getTargetListeningBand() != null) user.setTargetListeningBand(request.getTargetListeningBand());
        if (request.getTargetReadingBand() != null) user.setTargetReadingBand(request.getTargetReadingBand());
        if (request.getTargetWritingBand() != null) user.setTargetWritingBand(request.getTargetWritingBand());
        if (request.getTargetSpeakingBand() != null) user.setTargetSpeakingBand(request.getTargetSpeakingBand());
        if (request.getExamDate() != null) user.setExamDate(request.getExamDate());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getIsActive() != null) user.setIsActive(request.getIsActive());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        User saved = userRepository.save(user);

        return mapToResponseDto(saved);
    }

    @Override
    @Transactional
    public void delete(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        new ArrayList<>(user.getPracticeSubmissions())
                .forEach(practiceSubmission -> {
                    practiceSubmission.setUserId(null);
                    practiceSubmission.setUser(null);
                });

        new ArrayList<>(user.getPracticeContentProgresses())
                .forEach(progress -> {
                    progress.setUserId(null);
                    progress.setUser(null);
                });

        new ArrayList<>(user.getSubmissionFeedbacks())
                .forEach(feedback -> feedback.setAuthor(null));

        new ArrayList<>(user.getLearnerStudyPlans())
                .forEach(studyPlan -> studyPlan.setUser(null));

        new ArrayList<>(user.getSubmissionAnalytics())
                .forEach(analytics -> analytics.setUser(null));

        userRepository.flush();

        userRepository.delete(user);
    }

    private UserResponseDto mapToResponseDto(User saved) {
        UserResponseDto dto = new UserResponseDto();
        dto.setUserId(saved.getUserId());
        dto.setEmail(saved.getEmail());
        dto.setPhoneNumber(saved.getPhoneNumber());
        dto.setIsActive(saved.getIsActive());
        dto.setCreatedAt(saved.getCreatedAt());
        dto.setLastLoginAt(saved.getLastLoginAt());
        dto.setRole(saved.getRole());
        dto.setFirstname(saved.getFirstname());
        dto.setLastname(saved.getLastname());
        dto.setGender(saved.getGender());        
        dto.setDateOfBirth(saved.getDateOfBirth());
        dto.setCountry(saved.getCountry());
        dto.setTimezone(saved.getTimezone());
        dto.setAvatarUrl(saved.getAvatarUrl());
        dto.setTargetBand(saved.getTargetBand());
        dto.setExamDate(saved.getExamDate());
        return dto;
    }

    private void applyIncludes(User user, UserResponseDto dto, IncludeSpec includes) {
        if (includes.has("email")) dto.setEmail(user.getEmail());
        if (includes.has("phonenumber")) dto.setPhoneNumber(user.getPhoneNumber());
        if (includes.has("isactive")) dto.setIsActive(user.getIsActive());
        if (includes.has("createdat")) dto.setCreatedAt(user.getCreatedAt());
        if (includes.has("lastloginat")) dto.setLastLoginAt(user.getLastLoginAt());
        if (includes.has("role")) dto.setRole(user.getRole());
        if (includes.has("firstname")) dto.setFirstname(user.getFirstname());
        if (includes.has("lastname")) dto.setLastname(user.getLastname());
        if (includes.has("dateofbirth")) dto.setDateOfBirth(user.getDateOfBirth());
        if (includes.has("gender")) dto.setGender(user.getGender());
        if (includes.has("country")) dto.setCountry(user.getCountry());
        if (includes.has("timezone")) dto.setTimezone(user.getTimezone());
        if (includes.has("avatarurl")) dto.setAvatarUrl(user.getAvatarUrl());
        if (includes.has("targetband")) dto.setTargetBand(user.getTargetBand());
        if (includes.has("targetlisteningband")) dto.setTargetListeningBand(user.getTargetListeningBand());
        if (includes.has("targetreadingband")) dto.setTargetReadingBand(user.getTargetReadingBand());
        if (includes.has("targetwritingband")) dto.setTargetWritingBand(user.getTargetWritingBand());
        if (includes.has("targetspeakingband")) dto.setTargetSpeakingBand(user.getTargetSpeakingBand());
        if (includes.has("examdate")) dto.setExamDate(user.getExamDate());
    }
}
