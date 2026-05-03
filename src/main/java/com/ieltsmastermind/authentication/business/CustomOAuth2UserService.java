package com.ieltsmastermind.authentication.business;

import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.domain.enums.AuthProvider;
import com.ieltsmastermind.user.management.persistence.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(request);

        String registrationId = request.getClientRegistration().getRegistrationId(); // google / facebook

        String rawProviderId;
        String rawFirstName = null;
        String rawLastName = null;
        String email = oAuth2User.getAttribute("email");

        if ("google".equals(registrationId)) {
            rawProviderId = oAuth2User.getAttribute("sub");
            rawFirstName = oAuth2User.getAttribute("given_name");
            rawLastName = oAuth2User.getAttribute("family_name");
        } else if ("facebook".equals(registrationId)) {
            rawProviderId = oAuth2User.getAttribute("id");
            rawFirstName = oAuth2User.getAttribute("first_name");
            rawLastName = oAuth2User.getAttribute("last_name");
        } else {
            throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        }

        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        // 👉 Tạo biến final để dùng trong lambda
        final String providerId = rawProviderId;
        final String firstName = rawFirstName;
        final String lastName = rawLastName;

        // 🔍 1. Tìm theo provider + providerId trước
        User user = userRepository
                .findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {

                    // 🔗 2. Nếu chưa có, thử link bằng email (nếu có)
                    if (email != null && !email.isBlank()) {
                        return userRepository.findByEmail(email)
                                .map(existingUser -> {
                                    existingUser.setProvider(provider);
                                    existingUser.setProviderId(providerId);
                                    return userRepository.save(existingUser);
                                })
                                .orElseGet(() -> createNewOauthUser(email, firstName, lastName, provider, providerId));
                    }

                    // 3. Không có email → tạo user mới
                    return createNewOauthUser(null, firstName, lastName, provider, providerId);
                });

        return oAuth2User;
    }

    private User createNewOauthUser(String email, String firstName, String lastName,
                                    AuthProvider provider, String providerId) {

        User user = new User();
        user.setEmail(email); // có thể null
        user.setFirstname(firstName != null ? firstName : "User");
        user.setLastname(lastName != null ? lastName : "Social");
        user.setRole("Learner");

        user.setProvider(provider);
        user.setProviderId(providerId);
        user.setPasswordHash(null); // OAuth không dùng password


        return userRepository.save(user);
    }
}
