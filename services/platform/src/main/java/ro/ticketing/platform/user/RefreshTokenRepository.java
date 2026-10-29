package ro.ticketing.platform.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * SELECT ... FOR UPDATE: two requests refreshing with the same token run one after the other,
     * so the second one sees the token already rotated instead of both rotating it.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findForUpdateByTokenHash(String tokenHash);

    /** One UPDATE for the whole family; @UpdateTimestamp does not run on bulk updates, so updatedAt is set here. */
    @Modifying
    @Query("""
            update RefreshToken t
            set t.revokedAt = :now, t.revokeReason = :reason, t.updatedAt = :now
            where t.familyId = :familyId and t.revokedAt is null
            """)
    int revokeFamily(@Param("familyId") UUID familyId, @Param("reason") RevokeReason reason,
            @Param("now") Instant now);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
