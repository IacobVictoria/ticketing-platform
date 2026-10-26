package ro.ticketing.platform.event;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
class EventService {

    private final EventRepository eventRepository;
    private final Clock clock;

    EventService(EventRepository eventRepository, Clock clock) {
        this.eventRepository = eventRepository;
        this.clock = clock;
    }

    @Transactional
    Event create(String name, String description, Instant startsAt) {
        if (!startsAt.isAfter(clock.instant())) {
            throw new EventStartInPastException(startsAt);
        }
        return eventRepository.save(new Event(name, description, startsAt));
    }

    Event getById(UUID id) {
        return eventRepository.findById(id).orElseThrow(() -> new EventNotFoundException(id));
    }

    List<Event> listPublished() {
        return eventRepository.findByStatusOrderByStartsAtAsc(EventStatus.PUBLISHED);
    }

    @Transactional
    Event publish(UUID id) {
        Event event = getById(id);
        event.publish();
        return event;
    }

    @Transactional
    Event cancel(UUID id) {
        Event event = getById(id);
        event.cancel();
        return event;
    }
}
