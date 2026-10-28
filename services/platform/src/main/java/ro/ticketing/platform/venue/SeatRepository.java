package ro.ticketing.platform.venue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface SeatRepository extends JpaRepository<Seat, UUID> {

    /** Shorter labels first, then alphabetical: rows 1, 2, 10 (not 1, 10, 2) and A, B, Z, AA. */
    @Query("""
            select s from Seat s
            where s.sector.id = :sectorId
            order by length(s.rowLabel), s.rowLabel, s.seatNumber
            """)
    List<Seat> findInRowOrder(@Param("sectorId") UUID sectorId);

    boolean existsBySectorIdAndRowLabel(UUID sectorId, String rowLabel);

    long countBySectorId(UUID sectorId);

    /** One query for all sectors of a venue, instead of one count query per sector (N+1). */
    @Query("""
            select new ro.ticketing.platform.venue.SectorSeatCount(s.sector.id, count(s))
            from Seat s
            where s.sector.venue.id = :venueId
            group by s.sector.id
            """)
    List<SectorSeatCount> countSeatsBySector(@Param("venueId") UUID venueId);
}
