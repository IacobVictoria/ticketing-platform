package ro.ticketing.platform.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Instant startsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    protected Event() {
    }

    Event(String name, String description, Instant startsAt) {
        this.name = name;
        this.description = description;
        this.startsAt = startsAt;
        this.status = EventStatus.DRAFT;
    }

    void publish() {
        if (status != EventStatus.DRAFT) {
            throw new InvalidEventStateException(id, status, "publish");
        }
        status = EventStatus.PUBLISHED;
    }

    void cancel() {
        if (status == EventStatus.CANCELLED) {
            throw new InvalidEventStateException(id, status, "cancel");
        }
        status = EventStatus.CANCELLED;
    }

    UUID getId() {
        return id;
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

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
