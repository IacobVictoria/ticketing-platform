package ro.ticketing.platform.venue;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/venues")
class VenueController {

    private final VenueService venueService;

    VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @PostMapping
    ResponseEntity<VenueResponse> create(@Valid @RequestBody CreateVenueRequest request,
            UriComponentsBuilder uriBuilder) {
        Venue venue = venueService.create(request.name(), request.city(), request.address());
        URI location = uriBuilder.path("/api/venues/{id}").buildAndExpand(venue.getId()).toUri();
        return ResponseEntity.created(location).body(VenueResponse.from(venue));
    }

    @GetMapping
    List<VenueResponse> list() {
        return venueService.list().stream().map(VenueResponse::from).toList();
    }

    @GetMapping("/{venueId}")
    VenueDetailsResponse get(@PathVariable UUID venueId) {
        return VenueDetailsResponse.from(venueService.getDetails(venueId));
    }

    @PostMapping("/{venueId}/sectors")
    ResponseEntity<SectorResponse> addSector(@PathVariable UUID venueId,
            @Valid @RequestBody CreateSectorRequest request, UriComponentsBuilder uriBuilder) {
        Sector sector = venueService.addSector(venueId, request.name(), request.kind(), request.capacity());
        URI location = uriBuilder.path("/api/venues/{venueId}/sectors/{sectorId}")
                .buildAndExpand(venueId, sector.getId()).toUri();
        return ResponseEntity.created(location).body(SectorResponse.from(sector, 0));
    }

    @GetMapping("/{venueId}/sectors/{sectorId}")
    SectorResponse getSector(@PathVariable UUID venueId, @PathVariable UUID sectorId) {
        Sector sector = venueService.getSector(venueId, sectorId);
        return SectorResponse.from(sector, venueService.seatCount(sectorId));
    }

    @PostMapping("/{venueId}/sectors/{sectorId}/rows")
    ResponseEntity<List<SeatResponse>> addRow(@PathVariable UUID venueId, @PathVariable UUID sectorId,
            @Valid @RequestBody AddRowRequest request, UriComponentsBuilder uriBuilder) {
        List<Seat> seats = venueService.addRow(venueId, sectorId, request.rowLabel(), request.seats());
        URI location = uriBuilder.path("/api/venues/{venueId}/sectors/{sectorId}/seats")
                .buildAndExpand(venueId, sectorId).toUri();
        return ResponseEntity.created(location).body(seats.stream().map(SeatResponse::from).toList());
    }

    @GetMapping("/{venueId}/sectors/{sectorId}/seats")
    List<SeatResponse> seats(@PathVariable UUID venueId, @PathVariable UUID sectorId) {
        return venueService.seatsOf(venueId, sectorId).stream().map(SeatResponse::from).toList();
    }
}
