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

interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    /** SELECT ... FOR UPDATE: everything that changes a user's verification state runs one at a time per user. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findForUpdateByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findForUpdateById(UUID id);

    boolean existsByEmail(String email);

    boolean existsByRole(UserRole role);

    /** Tokens of the deleted accounts disappear too (ON DELETE CASCADE). */
    @Modifying
    @Query("delete from User u where u.emailVerifiedAt is null and u.updatedAt < :time")
    int deleteUnverifiedUpdatedBefore(@Param("time") Instant time);
}
