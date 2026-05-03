package com.ieltsmastermind.authentication.business;

import com.ieltsmastermind.authentication.domain.entity.PasswordResetToken;
import com.ieltsmastermind.authentication.persistence.PasswordResetTokenRepository;
import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.domain.enums.AuthProvider;
import com.ieltsmastermind.user.management.persistence.UserRepository;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    @Autowired
    private SessionManager sessionManager;
    @Autowired
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository userRepository, PasswordResetTokenRepository tokenRepository, RedisTemplate<String, String> redisTemplate, JavaMailSender mailSender, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    public void sendResetCode(String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Email not found"));

            if (user.getProvider() != AuthProvider.LOCAL) {
                throw new RuntimeException(
                        "This account uses " + user.getProvider() + " login. Please sign in using that provider."
                );
            }

            String otp = String.format("%04d", new SecureRandom().nextInt(10_000));
            String key = "pwd_code:" + email;

            redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(5));
            sendOtpEmail(email, otp);

        } catch (Exception e) {
            throw new RuntimeException("SYSTEM_ERROR", e);
        }
    }

    public String verifyResetCode(String email, String code) {
        String key = "pwd_code:" + email;
        String storedOtp = redisTemplate.opsForValue().get(key);

        System.out.println("=== VERIFY RESET CODE ===");
        System.out.println("Email: " + email);
        System.out.println("Input code: " + code);
        System.out.println("Stored code: " + storedOtp);

        if (storedOtp == null || !storedOtp.equals(code)) {
            throw new RuntimeException("Invalid or expired code");
        }

        redisTemplate.delete(key); // OTP chỉ dùng 1 lần

        String resetToken = UUID.randomUUID().toString();
        String resetKey = "pwd_reset:" + resetToken;

        redisTemplate.opsForValue().set(
                resetKey,
                email,
                Duration.ofMinutes(10)
        );

        return resetToken;
    }


    public void resetPassword(String resetToken, String newPassword) {
        String key = "pwd_reset:" + resetToken;
        String email = redisTemplate.opsForValue().get(key);

        if (email == null) {
            throw new RuntimeException("Invalid or expired reset token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new RuntimeException(
                    "This account uses " + user.getProvider() + " login and cannot reset password."
            );
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        sessionManager.invalidateAllSessionsOfUser(user.getUserId());
        redisTemplate.delete(key);
    }

    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password Reset Code");
        message.setText("Your verification code is: " + otp + ". It expires in 5 minutes.");
        mailSender.send(message);
    }
}
