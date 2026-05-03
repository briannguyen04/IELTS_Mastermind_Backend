package com.ieltsmastermind.authentication.business;

import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.domain.enums.AuthProvider;
import com.ieltsmastermind.user.management.persistence.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SessionManager sessionManager;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String registrationId =
                ((org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) authentication)
                        .getAuthorizedClientRegistrationId(); // "google" | "facebook"

        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        // ===== LẤY DỮ LIỆU TÙY PROVIDER =====
        String rawProviderId;
        String rawFirstName = null;
        String rawLastName = null;
        String rawEmail = oAuth2User.getAttribute("email"); // Facebook có thể null

        if (provider == AuthProvider.GOOGLE) {
            rawProviderId = oAuth2User.getAttribute("sub");
            rawFirstName = oAuth2User.getAttribute("given_name");
            rawLastName = oAuth2User.getAttribute("family_name");
        } else if (provider == AuthProvider.FACEBOOK) {
            rawProviderId = oAuth2User.getAttribute("id");
            rawFirstName = oAuth2User.getAttribute("first_name");
            rawLastName = oAuth2User.getAttribute("last_name");
        } else {
            throw new IllegalStateException("Unsupported provider: " + provider);
        }

        // ===== FINAL VARIABLES FOR LAMBDA =====
        final String providerId = rawProviderId;
        final String firstName = rawFirstName;
        final String lastName = rawLastName;
        final String email = rawEmail;

        // ===== TÌM HOẶC TẠO USER =====
        User user = userRepository
                .findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setProvider(provider);
                    newUser.setProviderId(providerId);
                    newUser.setEmail(email); // có thể null
                    newUser.setFirstname(firstName != null ? firstName : "User");
                    newUser.setLastname(lastName != null ? lastName : "Social");
                    newUser.setRole("Learner");
                    newUser.setPasswordHash(null); // OAuth user không có password
                    newUser.setIsActive(true);
                    return userRepository.save(newUser);
                });

        // ===== TẠO JWT =====
        String token = jwtUtils.generateToken(user.getUserId(), user.getRole());

        sessionManager.addSession(
                token,
                user.getUserId(),
                user.getRole(),
                jwtUtils.getExpirationMillis()
        );

        int maxAgeSeconds = (int) (jwtUtils.getExpirationMillis() / 1000);

        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // đổi thành true khi deploy HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);

        response.addCookie(cookie);

        // ===== REDIRECT VỀ FRONTEND =====
        response.sendRedirect(frontendUrl + "/oauth2/callback");
    }
}
