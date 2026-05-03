package com.ieltsmastermind.user.management.business.interfaces;

import com.ieltsmastermind.common.query.IncludeSpec;
import com.ieltsmastermind.user.management.domain.dto.UserCreateRequestDto;
import com.ieltsmastermind.user.management.domain.dto.UserResponseDto;
import com.ieltsmastermind.user.management.domain.dto.UserUpdateRequestDto;

import java.util.List;

public interface UserService {

    UserResponseDto create(UserCreateRequestDto request);

    List<UserResponseDto> getAll(IncludeSpec includes);

    UserResponseDto getById(String id, IncludeSpec includes);

    UserResponseDto update(String id, UserUpdateRequestDto request);

    void delete(String id);
}
