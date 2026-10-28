package ro.ticketing.platform.venue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SectorRepository extends JpaRepository<Sector, UUID> {

    List<Sector> findByVenueIdOrderByNameAsc(UUID venueId);

    Optional<Sector> findByIdAndVenueId(UUID id, UUID venueId);

    boolean existsByVenueIdAndNameIgnoreCase(UUID venueId, String name);
}
