package com.ieltsmastermind.authentication.business;

import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.domain.enums.AuthProvider;
import com.ieltsmastermind.user.management.persistence.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
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
                ((OAuth2AuthenticationToken) authentication)
                        .getAuthorizedClientRegistrationId();

        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        String rawProviderId;
        String rawFirstName = null;
        String rawLastName = null;
        String rawEmail = oAuth2User.getAttribute("email");

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

        final String providerId = rawProviderId;
        final String firstName = rawFirstName;
        final String lastName = rawLastName;
        final String email = rawEmail;

        User user = userRepository
                .findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setProvider(provider);
                    newUser.setProviderId(providerId);
                    newUser.setEmail(email);
                    newUser.setFirstname(firstName != null ? firstName : "User");
                    newUser.setLastname(lastName != null ? lastName : "Social");
                    newUser.setRole("Learner");
                    newUser.setPasswordHash(null);
                    newUser.setIsActive(true);
                    return userRepository.save(newUser);
                });

        String token = jwtUtils.generateToken(user.getUserId(), user.getRole());

        sessionManager.addSession(
                token,
                user.getUserId(),
                user.getRole(),
                jwtUtils.getExpirationMillis()
        );

        long maxAgeSeconds = jwtUtils.getExpirationMillis() / 1000;

        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        response.sendRedirect(frontendUrl + "/oauth2/callback");
    }
}