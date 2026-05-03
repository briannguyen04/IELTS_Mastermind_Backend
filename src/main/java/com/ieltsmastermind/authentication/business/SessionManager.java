package com.ieltsmastermind.authentication.business;

import lombok.Getter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {

    private final StringRedisTemplate redisTemplate;

    public SessionManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String buildSessionKey(String token) {
        return "session:" + token;
    }

    private String buildUserSessionsKey(String userId) {
        return "user_sessions:" + userId;
    }

    /**
     * Lưu session khi login
     */
    public void addSession(String token, String userId, String role, long expirationMillis) {
        String sessionKey = buildSessionKey(token);

        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("userId", userId);
        sessionData.put("role", role);
        sessionData.put("loginAt", String.valueOf(System.currentTimeMillis()));

        // Lưu session info
        redisTemplate.opsForHash().putAll(sessionKey, sessionData);
        redisTemplate.expire(sessionKey, Duration.ofMillis(expirationMillis));

        // 🔥 Track danh sách session của user
        String userSessionsKey = buildUserSessionsKey(userId);
        redisTemplate.opsForSet().add(userSessionsKey, token);
        redisTemplate.expire(userSessionsKey, Duration.ofMillis(expirationMillis));
    }

    /**
     * Kiểm tra session còn hợp lệ không
     */
    public boolean isValid(String token) {
        return redisTemplate.hasKey(buildSessionKey(token));
    }

    /**
     * Logout 1 session
     */
    public void removeSession(String token) {
        String sessionKey = buildSessionKey(token);

        Object userId = redisTemplate.opsForHash().get(sessionKey, "userId");

        redisTemplate.delete(sessionKey);

        // Xóa token khỏi danh sách session của user
        if (userId != null) {
            redisTemplate.opsForSet().remove(buildUserSessionsKey(userId.toString()), token);
        }
    }

    /**
     * Logout toàn bộ thiết bị của user
     */
    public void invalidateAllSessionsOfUser(String userId) {
        String userSessionsKey = buildUserSessionsKey(userId);

        Set<String> tokens = redisTemplate.opsForSet().members(userSessionsKey);
        if (tokens == null || tokens.isEmpty()) return;

        for (String token : tokens) {
            redisTemplate.delete(buildSessionKey(token));
        }

        redisTemplate.delete(userSessionsKey);
    }
}