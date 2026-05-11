package com.soulbuddy.be.domain.user.repository;

import com.soulbuddy.be.domain.user.entity.User;
import com.soulbuddy.be.global.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
}
