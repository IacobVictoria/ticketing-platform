package ro.ticketing.platform.event;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import ro.ticketing.platform.TestcontainersConfiguration;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class EventApiTests {

    @Autowired
    MockMvcTester mvc;

    @Test
    void createReturns201WithLocationAndDraftEvent() {
        MvcTestResult result = createEvent("Concert", inDays(30));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).headers().containsHeader(HttpHeaders.LOCATION);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("DRAFT");
        assertThat(result).bodyJson().extractingPath("$.createdAt").isNotNull();
    }

    @Test
    void publishedEventAppearsInList() {
        String id = idOf(createEvent("Festival", inDays(20)));

        assertThat(mvc.post().uri("/api/events/{id}/publish", id))
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("PUBLISHED");
        assertThat(mvc.get().uri("/api/events"))
                .hasStatusOk()
                .bodyJson().extractingPath("$[*].id").asArray().contains(id);
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() {
        MvcTestResult result = postJson("""
                {"name": "  ", "startsAt": null}
                """);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray()
                .containsExactlyInAnyOrder("name", "startsAt");
    }

    @Test
    void malformedJsonReturns400() {
        MvcTestResult result = postJson("{not json");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("BAD_REQUEST");
    }

    @Test
    void startInPastReturns422() {
        MvcTestResult result = createEvent("Old", Instant.now().minus(Duration.ofDays(1)));

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("EVENT_START_IN_PAST");
    }

    @Test
    void unknownEventReturns404WithProblemDetail() {
        UUID id = UUID.randomUUID();

        MvcTestResult result = mvc.get().uri("/api/events/{id}", id).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("EVENT_NOT_FOUND");
        assertThat(result).bodyJson().extractingPath("$.eventId").isEqualTo(id.toString());
        assertThat(result).bodyJson().extractingPath("$.instance").isEqualTo("/api/events/" + id);
        assertThat(result).bodyJson().extractingPath("$.timestamp").isNotNull();
    }

    @Test
    void invalidUuidReturns400() {
        assertThat(mvc.get().uri("/api/events/not-a-uuid"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("BAD_REQUEST");
    }

    @Test
    void publishingTwiceReturns409WithCurrentStatus() {
        String id = idOf(createEvent("Concert", inDays(30)));
        mvc.post().uri("/api/events/{id}/publish", id).exchange();

        MvcTestResult result = mvc.post().uri("/api/events/{id}/publish", id).exchange();

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("EVENT_INVALID_STATE");
        assertThat(result).bodyJson().extractingPath("$.currentStatus").isEqualTo("PUBLISHED");
    }

    @Test
    void unknownUrlReturns404ProblemDetail() {
        assertThat(mvc.get().uri("/api/does-not-exist"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.code").isEqualTo("NOT_FOUND");
    }

    private MvcTestResult createEvent(String name, Instant startsAt) {
        return postJson("""
                {"name": "%s", "startsAt": "%s"}
                """.formatted(name, startsAt));
    }

    private MvcTestResult postJson(String json) {
        return mvc.post().uri("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private static String idOf(MvcTestResult result) {
        String body = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.id");
    }

    private static Instant inDays(int days) {
        return Instant.now().plus(Duration.ofDays(days));
    }
}
