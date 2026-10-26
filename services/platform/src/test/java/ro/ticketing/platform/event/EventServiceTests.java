package ro.ticketing.platform.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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

    @Test
    void createdEventStartsAsDraft() {
        Event event = eventService.create("Concert", "Open air", inDays(30));

        Event loaded = eventService.getById(event.getId());
        assertThat(loaded.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(loaded.getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsEventInThePast() {
        assertThatThrownBy(() -> eventService.create("Old", null, Instant.now().minusSeconds(60)))
                .isInstanceOf(EventStartInPastException.class);
    }

    @Test
    void publishedEventsAreListedByStartDate() {
        Event later = eventService.create("Later", null, inDays(60));
        Event sooner = eventService.create("Sooner", null, inDays(10));
        Event draft = eventService.create("Draft", null, inDays(5));
        eventService.publish(later.getId());
        eventService.publish(sooner.getId());

        assertThat(eventService.listPublished())
                .extracting(Event::getId)
                .containsSubsequence(sooner.getId(), later.getId())
                .doesNotContain(draft.getId());
    }

    @Test
    void cannotPublishTwice() {
        Event event = eventService.create("Concert", null, inDays(30));
        eventService.publish(event.getId());

        assertThatThrownBy(() -> eventService.publish(event.getId()))
                .isInstanceOf(InvalidEventStateException.class);
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
