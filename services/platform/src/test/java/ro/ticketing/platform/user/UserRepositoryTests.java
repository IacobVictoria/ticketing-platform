package ro.ticketing.platform.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ro.ticketing.platform.TestcontainersConfiguration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UserRepositoryTests {

    @Autowired
    UserRepository userRepository;

    @Test
    void savedUserIsFoundByNormalizedEmail() {
        String email = "Ana." + UUID.randomUUID() + "@Example.RO";
        userRepository.save(new User("  " + email + " ", "hash", " Ana ", " Pop ", UserRole.USER));

        User found = userRepository.findByEmail(User.normalizeEmail(email)).orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getEmail()).isEqualTo(email.toLowerCase());
        assertThat(found.getFirstName()).isEqualTo("Ana");
        assertThat(found.getLastName()).isEqualTo("Pop");
        assertThat(found.getRole()).isEqualTo(UserRole.USER);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(userRepository.existsByEmail(User.normalizeEmail(email))).isTrue();
    }

    @Test
    void unknownEmailIsNotFound() {
        String email = "nobody." + UUID.randomUUID() + "@example.ro";

        assertThat(userRepository.findByEmail(email)).isEmpty();
        assertThat(userRepository.existsByEmail(email)).isFalse();
    }
}
