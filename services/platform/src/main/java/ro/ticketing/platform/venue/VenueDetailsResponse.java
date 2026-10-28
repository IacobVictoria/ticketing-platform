package ro.ticketing.platform.venue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

record VenueDetailsResponse(UUID id, String name, String city, String address, long totalCapacity,
        List<SectorResponse> sectors) {

    static VenueDetailsResponse from(VenueDetails details) {
        Map<UUID, Long> seatCounts = details.seatCounts();
        List<SectorResponse> sectorResponses = details.sectors().stream()
                .map(sector -> SectorResponse.from(sector, seatCounts.getOrDefault(sector.getId(), 0L)))
                .toList();
        long totalCapacity = sectorResponses.stream().mapToLong(SectorResponse::capacity).sum();
        Venue venue = details.venue();
        return new VenueDetailsResponse(venue.getId(), venue.getName(), venue.getCity(), venue.getAddress(),
                totalCapacity, sectorResponses);
    }
}
