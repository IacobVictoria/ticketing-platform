package ro.ticketing.platform.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import ro.ticketing.platform.common.BaseEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
class Event extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Instant startsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    /** Venues live in another module, so we keep only their id (the DB still has a foreign key). */
    private UUID venueId;

    protected Event() {
    }

    Event(String name, String description, Instant startsAt, UUID venueId) {
        this.name = name;
        this.description = description;
        this.startsAt = startsAt;
        this.venueId = venueId;
        this.status = EventStatus.DRAFT;
    }

    void publish() {
        if (status != EventStatus.DRAFT) {
            throw new InvalidEventStateException(getId(), status, "publish");
        }
        if (venueId == null) {
            throw new EventVenueMissingException(getId());
        }
        status = EventStatus.PUBLISHED;
    }

    /** Only a draft can get or change its venue; once published, tickets may already be tied to it. */
    void assignVenue(UUID venueId) {
        if (status != EventStatus.DRAFT) {
            throw new InvalidEventStateException(getId(), status, "change the venue of");
        }
        this.venueId = venueId;
    }

    void cancel() {
        if (status == EventStatus.CANCELLED) {
            throw new InvalidEventStateException(getId(), status, "cancel");
        }
        status = EventStatus.CANCELLED;
    }

    String getName() {
        return name;
    }

    String getDescription() {
        return description;
    }

    Instant getStartsAt() {
        return startsAt;
    }

    EventStatus getStatus() {
        return status;
    }

    UUID getVenueId() {
        return venueId;
    }
}
