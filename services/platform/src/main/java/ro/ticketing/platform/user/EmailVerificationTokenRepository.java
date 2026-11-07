package ro.ticketing.platform.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    boolean existsByUserIdAndCreatedAtAfter(UUID userId, Instant time);

    @Modifying
    @Query("delete from EmailVerificationToken t where t.userId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from EmailVerificationToken t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
