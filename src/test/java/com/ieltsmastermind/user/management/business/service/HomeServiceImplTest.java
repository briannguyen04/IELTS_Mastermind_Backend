package com.ieltsmastermind.user.management.business.service;

import com.ieltsmastermind.authentication.business.JwtUtils;
import com.ieltsmastermind.user.management.domain.dto.HomePageResponseDto;
import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.persistence.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private HomeServiceImpl homeService;

    @Test
    void getHomeUserInfo_whenJwtCookieIsMissing_shouldReturnGuestInfoAndSkipUserLookup() {
        when(jwtUtils.getJwtFromCookie(request)).thenReturn(null);

        HomePageResponseDto result = homeService.getHomeUserInfo(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNull();
        assertThat(result.getEmail()).isNull();
        assertThat(result.getFullName()).isNull();
        assertThat(result.getRole()).isEqualTo("guest");
        assertThat(result.getAvatarUrl()).isNull();

        verify(jwtUtils).getJwtFromCookie(request);
        verify(jwtUtils, never()).getUserIdFromToken(anyString());
        verify(userRepository, never()).findById(anyString());
    }

    @Test
    void getHomeUserInfo_whenTokenExistsAndUserDoesNotExist_shouldReturnGuestInfo() {
        String token = "jwt-token";
        String userId = "missing-user";

        when(jwtUtils.getJwtFromCookie(request)).thenReturn(token);
        when(jwtUtils.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        HomePageResponseDto result = homeService.getHomeUserInfo(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNull();
        assertThat(result.getEmail()).isNull();
        assertThat(result.getFullName()).isNull();
        assertThat(result.getRole()).isEqualTo("guest");
        assertThat(result.getAvatarUrl()).isNull();

        verify(jwtUtils).getJwtFromCookie(request);
        verify(jwtUtils).getUserIdFromToken(token);
        verify(userRepository).findById(userId);
    }

    @Test
    void getHomeUserInfo_whenTokenExistsAndUserExists_shouldReturnHomeUserInfo() {
        String token = "jwt-token";
        String userId = "user-1";
        User user = fullUser(userId);

        when(jwtUtils.getJwtFromCookie(request)).thenReturn(token);
        when(jwtUtils.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        HomePageResponseDto result = homeService.getHomeUserInfo(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getFullName()).isEqualTo("John Doe");
        assertThat(result.getRole()).isEqualTo("Learner");
        assertThat(result.getAvatarUrl()).isEqualTo("avatar.png");

        verify(jwtUtils).getJwtFromCookie(request);
        verify(jwtUtils).getUserIdFromToken(token);
        verify(userRepository).findById(userId);
    }

    @Test
    void getHomeUserInfo_whenUserHasEmptyNameFields_shouldReturnFullNameWithSpaceSeparator() {
        String token = "jwt-token";
        String userId = "user-1";
        User user = fullUser(userId);
        user.setFirstname("");
        user.setLastname("");

        when(jwtUtils.getJwtFromCookie(request)).thenReturn(token);
        when(jwtUtils.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        HomePageResponseDto result = homeService.getHomeUserInfo(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getFullName()).isEqualTo(" ");

        verify(jwtUtils).getJwtFromCookie(request);
        verify(jwtUtils).getUserIdFromToken(token);
        verify(userRepository).findById(userId);
    }

    @Test
    void getHomeUserInfo_whenRepositoryThrowsRuntimeException_shouldPropagateException() {
        String token = "jwt-token";
        String userId = "user-1";

        when(jwtUtils.getJwtFromCookie(request)).thenReturn(token);
        when(jwtUtils.getUserIdFromToken(token)).thenReturn(userId);
        when(userRepository.findById(userId)).thenThrow(new RuntimeException("Database unavailable"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> homeService.getHomeUserInfo(request)
        );

        assertThat(exception.getMessage()).isEqualTo("Database unavailable");

        verify(jwtUtils).getJwtFromCookie(request);
        verify(jwtUtils).getUserIdFromToken(token);
        verify(userRepository).findById(userId);
    }

    private User fullUser(String userId) {
        User user = new User();

        user.setUserId(userId);
        user.setEmail("john@example.com");
        user.setFirstname("John");
        user.setLastname("Doe");
        user.setRole("Learner");
        user.setAvatarUrl("avatar.png");

        return user;
    }
}
