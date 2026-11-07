package ro.ticketing.platform;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** A throwaway PostgreSQL, for the tests and for {@link TestPlatformApplication}. */
@TestConfiguration(proxyBeanMethods = false)
class PostgresContainerConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18"));
    }

}
