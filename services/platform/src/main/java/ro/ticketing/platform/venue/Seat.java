package ro.ticketing.platform.venue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ro.ticketing.platform.common.BaseEntity;

import java.util.Locale;

// visibility venue package only
@Entity
@Table(name = "seats")
class Seat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @Column(nullable = false, length = 10)
    private String rowLabel;

    @Column(nullable = false)
    private int seatNumber;

    protected Seat() {
    }

    Seat(Sector sector, String rowLabel, int seatNumber) {
        this.sector = sector;
        this.rowLabel = normalizeRowLabel(rowLabel);
        this.seatNumber = seatNumber;
    }

    /** Row labels are stored trimmed and upper-case, so "a", "A" and " A " are the same row. */
    static String normalizeRowLabel(String rowLabel) {
        return rowLabel.strip().toUpperCase(Locale.ROOT);
    }

    String getRowLabel() {
        return rowLabel;
    }

    int getSeatNumber() {
        return seatNumber;
    }
}
