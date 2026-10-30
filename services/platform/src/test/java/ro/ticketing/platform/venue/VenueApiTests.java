package ro.ticketing.platform.venue;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ro.ticketing.platform.TestcontainersConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class VenueApiTests {

    /** These tests are about events/venues, not security: every request comes from an admin. */
    static final RequestPostProcessor ADMIN = jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));

    @Autowired
    MockMvcTester mvc;

    String venueId;

    @BeforeEach
    void createVenue() {
        MvcTestResult result = post("/api/venues", """
                {"name": "Arena Nationala", "city": "Bucuresti", "address": "Bd. Basarabia 37-39"}
                """);
        assertThat(result).hasStatus(HttpStatus.CREATED);
        venueId = idOf(result);
    }

    @Test
    void buildsVenueWithSeatedAndStandingSectors() {
        String tribune = idOf(addSector("Tribuna 1", "SEATED", null));
        addSector("Gazon", "STANDING", 20000);

        MvcTestResult rowA = post(rowsUri(tribune), """
                {"rowLabel": "A", "seats": 10}
                """);
        assertThat(rowA).hasStatus(HttpStatus.CREATED);
        assertThat(rowA).headers().containsHeader(HttpHeaders.LOCATION);
        assertThat(rowA).bodyJson().extractingPath("$.length()").isEqualTo(10);
        post(rowsUri(tribune), """
                {"rowLabel": "B", "seats": 5}
                """);

        MvcTestResult venue = mvc.get().with(ADMIN).uri("/api/venues/{id}", venueId).exchange();
        assertThat(venue).hasStatusOk();
        assertThat(venue).bodyJson().extractingPath("$.totalCapacity").isEqualTo(20015);
        assertThat(venue).bodyJson().extractingPath("$.sectors[?(@.name == 'Tribuna 1')].capacity")
                .asArray().containsExactly(15);
        assertThat(venue).bodyJson().extractingPath("$.sectors[?(@.name == 'Gazon')].capacity")
                .asArray().containsExactly(20000);
    }

    @Test
    void seatsAreListedByRowAndNumber() {
        String sector = idOf(addSector("Tribuna 2", "SEATED", null));
        post(rowsUri(sector), """
                {"rowLabel": "B", "seats": 2}
                """);
        post(rowsUri(sector), """
                {"rowLabel": "A", "seats": 2}
                """);

        assertThat(mvc.get().with(ADMIN).uri("/api/venues/{v}/sectors/{s}/seats", venueId, sector))
                .hasStatusOk()
                .bodyJson().extractingPath("$[*].rowLabel").asArray().containsExactly("A", "A", "B", "B");
    }

    @Test
    void numericRowsAreListedInNumericOrder() {
        String sector = idOf(addSector("Tribuna 3", "SEATED", null));
        for (String row : new String[] {"10", "2", "1"}) {
            post(rowsUri(sector), """
                    {"rowLabel": "%s", "seats": 1}
                    """.formatted(row));
        }

        assertThat(mvc.get().with(ADMIN).uri("/api/venues/{v}/sectors/{s}/seats", venueId, sector))
                .hasStatusOk()
                .bodyJson().extractingPath("$[*].rowLabel").asArray().containsExactly("1", "2", "10");
    }

    @Test
    void rowLabelsIgnoreCaseAndSpaces() {
        String sector = idOf(addSector("Tribuna 1", "SEATED", null));

        assertThat(post(rowsUri(sector), """
                {"rowLabel": " a ", "seats": 2}
                """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().extractingPath("$[0].rowLabel").isEqualTo("A");
        assertThat(post(rowsUri(sector), """
                {"rowLabel": "A", "seats": 2}
                """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("ROW_ALREADY_EXISTS");
    }

    @Test
    void sectorNamesIgnoreCaseAndSpaces() {
        assertThat(addSector(" Tribuna 1 ", "SEATED", null))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().extractingPath("$.name").isEqualTo("Tribuna 1");
        assertThat(addSector("tribuna 1", "SEATED", null))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("SECTOR_ALREADY_EXISTS");
    }

    @Test
    void standingSectorWithoutCapacityReturns422() {
        assertThat(addSector("Gazon", "STANDING", null))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.code").isEqualTo("SECTOR_CAPACITY_INVALID");
    }

    @Test
    void duplicateSectorNameReturns409() {
        addSector("Tribuna 1", "SEATED", null);

        assertThat(addSector("Tribuna 1", "SEATED", null))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("SECTOR_ALREADY_EXISTS");
    }

    @Test
    void duplicateRowReturns409() {
        String sector = idOf(addSector("Tribuna 1", "SEATED", null));
        post(rowsUri(sector), """
                {"rowLabel": "A", "seats": 10}
                """);

        assertThat(post(rowsUri(sector), """
                {"rowLabel": "A", "seats": 3}
                """))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("ROW_ALREADY_EXISTS");
    }

    @Test
    void rowsOnStandingSectorReturn422() {
        String gazon = idOf(addSector("Gazon", "STANDING", 5000));

        assertThat(post(rowsUri(gazon), """
                {"rowLabel": "A", "seats": 10}
                """))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.code").isEqualTo("SECTOR_NOT_SEATED");
    }

    @Test
    void sectorOfAnotherVenueReturns404() {
        String sector = idOf(addSector("Tribuna 1", "SEATED", null));
        String otherVenue = idOf(post("/api/venues", """
                {"name": "Sala Polivalenta", "city": "Bucuresti"}
                """));

        assertThat(mvc.get().with(ADMIN).uri("/api/venues/{v}/sectors/{s}", otherVenue, sector))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.code").isEqualTo("SECTOR_NOT_FOUND");
    }

    @Test
    void unknownVenueReturns404() {
        assertThat(mvc.get().with(ADMIN).uri("/api/venues/{id}", UUID.randomUUID()))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.code").isEqualTo("VENUE_NOT_FOUND");
    }

    @Test
    void invalidSectorKindReturns400() {
        assertThat(post("/api/venues/" + venueId + "/sectors", """
                {"name": "VIP", "kind": "FLOATING"}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("BAD_REQUEST");
    }

    private MvcTestResult addSector(String name, String kind, Integer capacity) {
        return post("/api/venues/" + venueId + "/sectors", """
                {"name": "%s", "kind": "%s", "capacity": %s}
                """.formatted(name, kind, capacity));
    }

    private String rowsUri(String sectorId) {
        return "/api/venues/" + venueId + "/sectors/" + sectorId + "/rows";
    }

    private MvcTestResult post(String uri, String json) {
        return mvc.post().with(ADMIN).uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private static String idOf(MvcTestResult result) {
        String body = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.id");
    }
}
