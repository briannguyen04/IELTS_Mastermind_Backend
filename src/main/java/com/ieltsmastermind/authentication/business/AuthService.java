package com.ieltsmastermind.authentication.business;

import com.ieltsmastermind.authentication.domain.dto.UserRegisterResponseDto;
import com.ieltsmastermind.user.management.domain.enums.AuthProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.ieltsmastermind.authentication.domain.dto.UserRegisterRequestDto;
import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.persistence.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private SessionManager sessionManager;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserRegisterResponseDto register(UserRegisterRequestDto request) {
        // check email
        userRepository.findByEmail(request.getEmail()).ifPresent(u -> {
            throw new RuntimeException("Email already exists");
        });

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstname(request.getFirstname());
        user.setLastname(request.getLastname());
        user.setRole("Learner");
        user.setProvider(AuthProvider.LOCAL);
        user.setProviderId(null);
        User saved = userRepository.save(user);

        return new UserRegisterResponseDto(
                saved.getEmail(),
                saved.getFirstname(),
                saved.getLastname()
        );
    }


    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Incorrect password");
        }
        String token = jwtUtils.generateToken(
                user.getUserId(),
                user.getRole()
        );
        sessionManager.addSession(token, user.getUserId(),user.getRole(), jwtUtils.getExpirationMillis());
        return token;
    }

    public void logout(String token) {
        if (token != null && sessionManager.isValid(token)) {
            sessionManager.removeSession(token);
        }
    }



}
