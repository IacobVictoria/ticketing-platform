package ro.ticketing.platform.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import ro.ticketing.platform.TestcontainersConfiguration;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EventServiceTests {

    @Autowired
    EventService eventService;

    @Autowired
    JdbcClient jdbcClient;

    UUID venueId;

    /** Venue creation is internal to the venue module, so test data is inserted directly. */
    @BeforeEach
    void createVenue() {
        venueId = UUID.randomUUID();
        jdbcClient.sql("""
                        insert into venues (id, name, city, created_at, updated_at)
                        values (:id, 'Arena Nationala', 'Bucuresti', now(), now())
                        """)
                .param("id", venueId)
                .update();
    }

    @Test
    void createdEventStartsAsDraft() {
        Event event = eventService.create("Concert", "Open air", inDays(30), venueId);

        Event loaded = eventService.getById(event.getId());
        assertThat(loaded.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(loaded.getVenueId()).isEqualTo(venueId);
        assertThat(loaded.getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsEventInThePast() {
        assertThatThrownBy(() -> eventService.create("Old", null, Instant.now().minusSeconds(60), venueId))
                .isInstanceOf(EventStartInPastException.class);
    }

    @Test
    void rejectsUnknownVenue() {
        assertThatThrownBy(() -> eventService.create("Concert", null, inDays(30), UUID.randomUUID()))
                .hasFieldOrPropertyWithValue("code", "VENUE_NOT_FOUND");
    }

    @Test
    void publishedEventsAreListedByStartDate() {
        Event later = eventService.create("Later", null, inDays(60), venueId);
        Event sooner = eventService.create("Sooner", null, inDays(10), venueId);
        Event draft = eventService.create("Draft", null, inDays(5), venueId);
        eventService.publish(later.getId());
        eventService.publish(sooner.getId());

        assertThat(eventService.listPublished())
                .extracting(Event::getId)
                .containsSubsequence(sooner.getId(), later.getId())
                .doesNotContain(draft.getId());
    }

    @Test
    void cannotPublishTwice() {
        Event event = eventService.create("Concert", null, inDays(30), venueId);
        eventService.publish(event.getId());

        assertThatThrownBy(() -> eventService.publish(event.getId()))
                .isInstanceOf(InvalidEventStateException.class);
    }

    @Test
    void cannotPublishWithoutVenue() {
        Event event = eventService.create("Concert", null, inDays(30), null);

        assertThatThrownBy(() -> eventService.publish(event.getId()))
                .isInstanceOf(EventVenueMissingException.class);
    }

    @Test
    void unknownIdThrowsNotFound() {
        assertThatThrownBy(() -> eventService.getById(UUID.randomUUID()))
                .isInstanceOf(EventNotFoundException.class);
    }

    private static Instant inDays(int days) {
        return Instant.now().plus(Duration.ofDays(days));
    }
}
