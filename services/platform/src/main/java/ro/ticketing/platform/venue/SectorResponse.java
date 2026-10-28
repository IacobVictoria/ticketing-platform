package ro.ticketing.platform.venue;

import java.util.UUID;

/** capacity = number of seats for SEATED sectors, the configured capacity for STANDING ones. */
record SectorResponse(UUID id, String name, SectorKind kind, long capacity) {

    static SectorResponse from(Sector sector, long seatCount) {
        long capacity = sector.isSeated() ? seatCount : sector.getCapacity();
        return new SectorResponse(sector.getId(), sector.getName(), sector.getKind(), capacity);
    }
}
