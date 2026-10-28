package ro.ticketing.platform.venue;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface VenueRepository extends JpaRepository<Venue, UUID> {

    List<Venue> findAllByOrderByNameAsc();

    /** SELECT ... FOR SHARE: others can still read the venue, but cannot delete it until our transaction ends. */
    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<Venue> findForShareById(UUID id);
}
