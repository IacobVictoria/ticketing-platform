package ro.ticketing.platform.venue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import ro.ticketing.platform.TestcontainersConfiguration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The database must reject invalid rows even when they bypass the Java code (e.g. a manual INSERT). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class VenueSchemaConstraintsTests {

    @Autowired
    JdbcClient jdbcClient;

    UUID venueId;

    @BeforeEach
    void createVenue() {
        venueId = UUID.randomUUID();
        jdbcClient.sql("insert into venues (id, name, city, created_at, updated_at) values (:id, 'V', 'C', now(), now())")
                .param("id", venueId)
                .update();
    }

    @Test
    void standingSectorWithNullCapacityIsRejected() {
        assertThatThrownBy(() -> insertSector("STANDING", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("sectors_capacity_check");
    }

    @Test
    void standingSectorWithZeroCapacityIsRejected() {
        assertThatThrownBy(() -> insertSector("STANDING", 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void seatedSectorWithCapacityIsRejected() {
        assertThatThrownBy(() -> insertSector("SEATED", 100))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void validSectorsAreAccepted() {
        assertThatCode(() -> insertSector("STANDING", 100)).doesNotThrowAnyException();
        assertThatCode(() -> insertSector("SEATED", null)).doesNotThrowAnyException();
    }

    @Test
    void sectorNamesDifferingOnlyInCaseAreRejected() {
        insertSector("Tribuna 1", "SEATED", null);

        assertThatThrownBy(() -> insertSector("tribuna 1", "SEATED", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("sectors_venue_name_unique");
    }

    @Test
    void untrimmedSectorNameIsRejected() {
        assertThatThrownBy(() -> insertSector(" Tribuna 1", "SEATED", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("sectors_name_trimmed_check");
    }

    @Test
    void lowercaseRowLabelIsRejected() {
        UUID sectorId = insertSector("SEATED", null);

        assertThatThrownBy(() -> insertSeat(sectorId, "a"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("seats_row_label_normalized_check");
        assertThatCode(() -> insertSeat(sectorId, "A")).doesNotThrowAnyException();
    }

    private UUID insertSector(String kind, Integer capacity) {
        return insertSector("Sector " + UUID.randomUUID(), kind, capacity);
    }

    private UUID insertSector(String name, String kind, Integer capacity) {
        UUID id = UUID.randomUUID();
        jdbcClient.sql("""
                        insert into sectors (id, venue_id, name, kind, capacity, created_at, updated_at)
                        values (:id, :venueId, :name, :kind, :capacity, now(), now())
                        """)
                .param("id", id)
                .param("venueId", venueId)
                .param("name", name)
                .param("kind", kind)
                .param("capacity", capacity)
                .update();
        return id;
    }

    private void insertSeat(UUID sectorId, String rowLabel) {
        jdbcClient.sql("""
                        insert into seats (id, sector_id, row_label, seat_number, created_at, updated_at)
                        values (:id, :sectorId, :rowLabel, 1, now(), now())
                        """)
                .param("id", UUID.randomUUID())
                .param("sectorId", sectorId)
                .param("rowLabel", rowLabel)
                .update();
    }
}
