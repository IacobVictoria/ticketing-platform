package ro.ticketing.platform.venue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ro.ticketing.platform.common.BaseEntity;

@Entity
@Table(name = "sectors")
class Sector extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SectorKind kind;

    private Integer capacity;

    protected Sector() {
    }

    Sector(Venue venue, String name, SectorKind kind, Integer capacity) {
        if (kind == SectorKind.STANDING && (capacity == null || capacity <= 0)) {
            throw new InvalidSectorCapacityException("A standing sector needs a positive capacity");
        }
        if (kind == SectorKind.SEATED && capacity != null) {
            throw new InvalidSectorCapacityException("A seated sector's capacity comes from its seats; do not set it");
        }
        this.venue = venue;
        this.name = normalizeName(name);
        this.kind = kind;
        this.capacity = capacity;
    }

    /** Names are stored trimmed; uniqueness per venue ignores case ("Tribuna 1" = "tribuna 1"). */
    static String normalizeName(String name) {
        return name.strip();
    }

    boolean isSeated() {
        return kind == SectorKind.SEATED;
    }

    String getName() {
        return name;
    }

    SectorKind getKind() {
        return kind;
    }

    Integer getCapacity() {
        return capacity;
    }
}
