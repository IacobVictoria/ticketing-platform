package ro.ticketing.platform.event;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByStatusOrderByStartsAtAsc(EventStatus status);
}
