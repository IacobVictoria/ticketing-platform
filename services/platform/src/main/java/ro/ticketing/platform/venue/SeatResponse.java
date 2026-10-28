package ro.ticketing.platform.venue;

import java.util.UUID;

record SeatResponse(UUID id, String rowLabel, int seatNumber) {

    static SeatResponse from(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getRowLabel(), seat.getSeatNumber());
    }
}
