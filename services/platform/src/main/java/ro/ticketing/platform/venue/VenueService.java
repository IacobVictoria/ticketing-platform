package ro.ticketing.platform.venue;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Only {@link #requireExists(UUID)} is public: it is the venue module's API for other modules (e.g. events).
 * Everything else is used by {@link VenueController} inside this package.
 */
@Service
@Transactional(readOnly = true)
public class VenueService {

    private final VenueRepository venueRepository;
    private final SectorRepository sectorRepository;
    private final SeatRepository seatRepository;

    VenueService(VenueRepository venueRepository, SectorRepository sectorRepository, SeatRepository seatRepository) {
        this.venueRepository = venueRepository;
        this.sectorRepository = sectorRepository;
        this.seatRepository = seatRepository;
    }

    /**
     * Fails with VENUE_NOT_FOUND if the venue does not exist. Otherwise the venue stays locked (FOR SHARE)
     * until the caller's transaction ends, so it cannot be deleted between this check and the caller's insert.
     * MANDATORY: the lock is only useful inside the caller's transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireExists(UUID venueId) {
        if (venueRepository.findForShareById(venueId).isEmpty()) {
            throw new VenueNotFoundException(venueId);
        }
    }

    @Transactional
    Venue create(String name, String city, String address) {
        return venueRepository.save(new Venue(name, city, address));
    }

    List<Venue> list() {
        return venueRepository.findAllByOrderByNameAsc();
    }

    Venue getById(UUID venueId) {
        return venueRepository.findById(venueId).orElseThrow(() -> new VenueNotFoundException(venueId));
    }

    /**
     * REPEATABLE_READ: all three queries see the same snapshot. With PostgreSQL's default (READ COMMITTED)
     * each query sees its own, so a row added in between could make sectors and seat counts disagree.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    VenueDetails getDetails(UUID venueId) {
        Venue venue = getById(venueId);
        List<Sector> sectors = sectorRepository.findByVenueIdOrderByNameAsc(venueId);
        Map<UUID, Long> seatCounts = seatRepository.countSeatsBySector(venueId).stream()
                .collect(Collectors.toMap(SectorSeatCount::sectorId, SectorSeatCount::seats));
        return new VenueDetails(venue, sectors, seatCounts);
    }

    @Transactional
    Sector addSector(UUID venueId, String name, SectorKind kind, Integer capacity) {
        Venue venue = getById(venueId);
        String sectorName = Sector.normalizeName(name);
        if (sectorRepository.existsByVenueIdAndNameIgnoreCase(venueId, sectorName)) {
            throw new SectorAlreadyExistsException(venueId, sectorName);
        }
        return sectorRepository.save(new Sector(venue, sectorName, kind, capacity));
    }

    Sector getSector(UUID venueId, UUID sectorId) {
        return sectorRepository.findByIdAndVenueId(sectorId, venueId)
                .orElseThrow(() -> new SectorNotFoundException(venueId, sectorId));
    }

    long seatCount(UUID sectorId) {
        return seatRepository.countBySectorId(sectorId);
    }

    /** Creates seats 1..seatCount in a new row of a seated sector. */
    @Transactional
    List<Seat> addRow(UUID venueId, UUID sectorId, String rowLabel, int seatCount) {
        Sector sector = getSector(venueId, sectorId);
        if (!sector.isSeated()) {
            throw new SectorNotSeatedException(sectorId);
        }
        String label = Seat.normalizeRowLabel(rowLabel);
        if (seatRepository.existsBySectorIdAndRowLabel(sectorId, label)) {
            throw new RowAlreadyExistsException(sectorId, label);
        }
        List<Seat> seats = IntStream.rangeClosed(1, seatCount)
                .mapToObj(number -> new Seat(sector, label, number))
                .toList();
        return seatRepository.saveAll(seats);
    }

    List<Seat> seatsOf(UUID venueId, UUID sectorId) {
        getSector(venueId, sectorId);
        return seatRepository.findInRowOrder(sectorId);
    }
}
