package com.ieltsmastermind.user.management.business.service;


import com.ieltsmastermind.authentication.business.JwtUtils;
import com.ieltsmastermind.user.management.business.interfaces.HomeService;
import com.ieltsmastermind.user.management.domain.dto.HomePageResponseDto;
import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.persistence.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class HomeServiceImpl implements HomeService {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    public HomeServiceImpl(UserRepository userRepository,
                           JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
    }

    public HomePageResponseDto getHomeUserInfo(HttpServletRequest request) {

        String token = jwtUtils.getJwtFromCookie(request);
        if (token == null) {
            return new HomePageResponseDto(null, null, null, "guest", null);
        }

        String userId = jwtUtils.getUserIdFromToken(token);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return new HomePageResponseDto(null, null, null, "guest", null);
        }

        return new HomePageResponseDto(
                user.getUserId(),
                user.getEmail(),
                user.getFirstname() + " " + user.getLastname(),
                user.getRole(),
                user.getAvatarUrl()
        );
    }
}
