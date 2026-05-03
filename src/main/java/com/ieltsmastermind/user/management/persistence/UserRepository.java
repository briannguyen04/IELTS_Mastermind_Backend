package com.ieltsmastermind.user.management.persistence;

import com.ieltsmastermind.user.management.domain.entity.User;
import com.ieltsmastermind.user.management.domain.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
