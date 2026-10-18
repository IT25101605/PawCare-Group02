package com.pawcare.demo.repository;

import com.pawcare.demo.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByUserIdAndTokenType(Long userId, String tokenType);
    void deleteByUserId(Long userId);
}
