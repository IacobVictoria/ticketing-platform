package ro.ticketing.platform.event;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
class EventController {

    private final EventService eventService;

    EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request,
            UriComponentsBuilder uriBuilder) {
        Event event = eventService.create(request.name(), request.description(), request.startsAt(),
                request.venueId());
        URI location = uriBuilder.path("/api/events/{id}").buildAndExpand(event.getId()).toUri();
        return ResponseEntity.created(location).body(EventResponse.from(event));
    }

    @GetMapping
    List<EventResponse> listPublished() {
        return eventService.listPublished().stream().map(EventResponse::from).toList();
    }

    /** Public route; only admins may see DRAFT events. isUserInRole("ADMIN") checks the ROLE_ADMIN authority. */
    @GetMapping("/{id}")
    EventResponse get(@PathVariable UUID id, HttpServletRequest request) {
        boolean isAdmin = request.isUserInRole("ADMIN");
        return EventResponse.from(eventService.getVisible(id, isAdmin));
    }

    @PostMapping("/{id}/publish")
    EventResponse publish(@PathVariable UUID id) {
        return EventResponse.from(eventService.publish(id));
    }

    @PutMapping("/{id}/venue")
    EventResponse assignVenue(@PathVariable UUID id, @Valid @RequestBody AssignVenueRequest request) {
        return EventResponse.from(eventService.assignVenue(id, request.venueId()));
    }

    @PostMapping("/{id}/cancel")
    EventResponse cancel(@PathVariable UUID id) {
        return EventResponse.from(eventService.cancel(id));
    }
}
