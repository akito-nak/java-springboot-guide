package testing;

// ============================================================
// 10 - TESTING IN SPRING BOOT
// ============================================================
// "Code without tests is broken by design." — Jacob Kaplan-Moss
//
// A good test suite is the difference between deploying confidently
// and deploying with fingers crossed. Spring Boot has excellent
// testing support built in. Learn the three levels:
//
//   Unit tests       → test one class in isolation (no Spring context)
//   Integration tests → test slices of Spring (e.g., just the web layer)
//   E2E tests        → test the whole application (real HTTP, real DB)
//
// Dependencies:
//   spring-boot-starter-test (includes JUnit 5, Mockito, AssertJ, MockMvc)
//   com.h2database:h2 (in-memory DB for tests)
//   org.testcontainers:junit-jupiter (real DB in Docker for integration tests)

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.autoconfigure.orm.jpa.*;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.http.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.context.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;

// ─────────────────────────────────────────────────────────────
// UNIT TESTS — fast, isolated, no Spring context
// ─────────────────────────────────────────────────────────────

@ExtendWith(MockitoExtension.class)     // enables @Mock, @InjectMocks
class UserServiceTest {

    @Mock
    private UserRepository userRepository;  // Mockito creates a fake

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;        // real service, with mocks injected

    @Test
    @DisplayName("createUser: should save user and send welcome email")
    void createUser_ShouldSaveAndSendEmail() {
        // GIVEN (setup)
        String name  = "Alice";
        String email = "alice@example.com";
        User savedUser = new User(1L, name, email);

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // WHEN (action)
        User result = userService.createUser(name, email);

        // THEN (assertions)
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo(name);
        assertThat(result.email()).isEqualTo(email);

        // Verify the email was sent exactly once
        verify(emailService, times(1)).sendWelcomeEmail(email, name);
        // Verify we never called delete
        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("createUser: should throw when email already exists")
    void createUser_DuplicateEmail_ShouldThrow() {
        // GIVEN
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // WHEN + THEN
        assertThatThrownBy(() -> userService.createUser("Bob", "existing@example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already registered");

        // Ensure we never tried to save
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("getUser: should throw ResourceNotFoundException for unknown id")
    void getUser_NotFound_ShouldThrow() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ArgumentCaptor — capture what was passed to a mock method
    @Test
    void createUser_ShouldSaveUserWithCorrectData() {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.createUser("Charlie", "charlie@example.com");

        verify(userRepository).save(userCaptor.capture());
        User captured = userCaptor.getValue();
        assertThat(captured.name()).isEqualTo("Charlie");
        assertThat(captured.email()).isEqualTo("charlie@example.com");
    }

    // ── Parameterised tests ──────────────────────────────────
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  \t  "})
    @DisplayName("createUser: should reject blank names")
    void createUser_BlankName_ShouldThrow(String name) {
        assertThatThrownBy(() -> userService.createUser(name, "valid@example.com"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({
        "Alice, 30, ACTIVE",
        "Bob,   25, ACTIVE",
        "Carol, 17, INACTIVE"
    })
    void userCreation_WithVariousInputs(String name, int age, String expectedStatus) {
        // parameterised test with multiple fields
    }

    @ParameterizedTest
    @MethodSource("provideInvalidEmails")
    void createUser_InvalidEmail_ShouldThrow(String email) {
        assertThatThrownBy(() -> userService.createUser("Valid Name", email))
            .isInstanceOf(IllegalArgumentException.class);
    }

    static Stream<String> provideInvalidEmails() {
        return java.util.stream.Stream.of("notanemail", "@nodomain", "no@", "", null);
    }

    // ── Nested tests — group related scenarios ───────────────
    @Nested
    @DisplayName("When user exists")
    class WhenUserExists {
        private User existingUser;

        @BeforeEach
        void setup() {
            existingUser = new User(1L, "Alice", "alice@example.com");
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        }

        @Test
        void getUser_ShouldReturnUser() {
            User found = userService.getUser(1L);
            assertThat(found).isEqualTo(existingUser);
        }

        @Test
        void updateUser_ShouldUpdateFields() {
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            // ... test update logic
        }
    }
}

// ─────────────────────────────────────────────────────────────
// WEB LAYER SLICE — @WebMvcTest
// ─────────────────────────────────────────────────────────────
// Only loads the web layer (controllers, filters, serialisers).
// Much faster than @SpringBootTest for testing HTTP behaviour.

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean  // registers a Mockito mock in the Spring context
    private UserService userService;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/users/{id} → 200 OK with user body")
    void getUser_ShouldReturn200() throws Exception {
        UserResponse user = new UserResponse(1L, "Alice", "alice@example.com",
            "ACTIVE", java.time.LocalDateTime.now());
        when(userService.getUser(1L)).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/1")
                .header("Authorization", "Bearer mock-jwt-token")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Alice"))
            .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} → 404 when not found")
    void getUser_NotFound_ShouldReturn404() throws Exception {
        when(userService.getUser(999L)).thenThrow(new ResourceNotFoundException("User", 999L));

        mockMvc.perform(get("/api/v1/users/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("POST /api/v1/users → 400 with validation errors for invalid request")
    void createUser_InvalidRequest_ShouldReturn400() throws Exception {
        String requestBody = """
            {
                "name": "",
                "email": "not-an-email",
                "password": "weak"
            }
            """;

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.name").exists())
            .andExpect(jsonPath("$.fieldErrors.email").exists())
            .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("POST /api/v1/users → 201 Created for valid request")
    void createUser_ValidRequest_ShouldReturn201() throws Exception {
        CreateUserRequest request = new CreateUserRequest("Alice", "alice@example.com", "Str0ng!Pass", 25);
        UserResponse created = new UserResponse(1L, "Alice", "alice@example.com",
            "ACTIVE", java.time.LocalDateTime.now());
        when(userService.createUser(any())).thenReturn(created);

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").value(1));
    }
}

// ─────────────────────────────────────────────────────────────
// DATA LAYER SLICE — @DataJpaTest
// ─────────────────────────────────────────────────────────────
// Loads only JPA repositories and H2 in-memory DB.
// No web layer, no service beans.

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager entityManager;

    private User savedUser;

    @BeforeEach
    void setup() {
        savedUser = entityManager.persistAndFlush(new User("Alice", "alice@example.com"));
    }

    @Test
    void findByEmail_ShouldReturnUser() {
        Optional<User> found = userRepository.findByEmail("alice@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Alice");
    }

    @Test
    void existsByEmail_ShouldReturnTrue() {
        assertThat(userRepository.existsByEmail("alice@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void findByStatus_ShouldReturnActiveUsers() {
        entityManager.persistAndFlush(new User("Bob", "bob@example.com"));

        List<User> activeUsers = userRepository.findByStatus(UserStatus.ACTIVE);
        assertThat(activeUsers).hasSize(2);
        assertThat(activeUsers).extracting("email")
            .containsExactlyInAnyOrder("alice@example.com", "bob@example.com");
    }
}

// ─────────────────────────────────────────────────────────────
// FULL INTEGRATION TEST — @SpringBootTest
// ─────────────────────────────────────────────────────────────
// Loads the FULL application context. Slow but thorough.
// Use sparingly — prefer slices for most tests.

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserIntegrationTest {

    @Autowired
    private org.springframework.boot.test.web.client.TestRestTemplate restTemplate;

    @Test
    @DisplayName("Full user creation flow")
    void createAndFetchUser_IntegrationTest() {
        CreateUserRequest request = new CreateUserRequest(
            "Alice", "alice@integration.com", "Str0ng!Pass", 25
        );

        // Create user
        var createResponse = restTemplate.postForEntity(
            "/api/v1/users", request, UserResponse.class
        );
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        Long userId = createResponse.getBody().id();

        // Fetch user
        var getResponse = restTemplate.getForEntity(
            "/api/v1/users/" + userId, UserResponse.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().email()).isEqualTo("alice@integration.com");
    }
}

// ─────────────────────────────────────────────────────────────
// TESTCONTAINERS — real PostgreSQL in Docker for tests
// ─────────────────────────────────────────────────────────────
// Add: org.testcontainers:junit-jupiter, org.testcontainers:postgresql

@SpringBootTest
@Testcontainers  // from org.testcontainers.junit.jupiter
class UserRepositoryWithRealDbTest {

    @Container  // spins up a real PostgreSQL Docker container
    static org.testcontainers.containers.PostgreSQLContainer<?> postgres =
        new org.testcontainers.containers.PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource  // override Spring properties dynamically
    static void configureProperties(
            org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndRetrieveFromRealDatabase() {
        User user = userRepository.save(new User("Alice", "alice@realdb.com"));
        assertThat(user.getId()).isNotNull();

        Optional<User> found = userRepository.findByEmail("alice@realdb.com");
        assertThat(found).isPresent();
    }
}

// ─────────────────────────────────────────────────────────────
// TEST UTILITIES — helper patterns
// ─────────────────────────────────────────────────────────────

// Builder for test data — avoids duplicated setup code
class UserTestBuilder {
    private Long id = 1L;
    private String name = "Test User";
    private String email = "test@example.com";

    static UserTestBuilder aUser() { return new UserTestBuilder(); }

    UserTestBuilder withId(Long id)       { this.id = id;     return this; }
    UserTestBuilder withName(String name) { this.name = name; return this; }
    UserTestBuilder withEmail(String email) { this.email = email; return this; }

    User build() { return new User(id, name, email); }
}

// Usage in tests:
// User user = UserTestBuilder.aUser().withName("Alice").withEmail("alice@test.com").build();

// ─────────────────────────────────────────────────────────────
// PLACEHOLDER IMPORTS AND TYPES for file to not fail to parse
// ─────────────────────────────────────────────────────────────

// These would normally be imported from their respective packages
class UserController {}
interface UserRepository {
    Optional<User> findByEmail(String email);
    List<User> findByStatus(UserStatus status);
    boolean existsByEmail(String email);
    User save(User user);
    Optional<User> findById(Long id);
}
interface UserService {
    User createUser(String name, String email);
    User getUser(Long id);
    UserResponse createUser(Object request);
    UserResponse updateUser(Object id, Object request);
    UserResponse getUser(Long id);
}
interface EmailService { void sendWelcomeEmail(String to, String name); }
record User(Long id, String name, String email) {
    User(String name, String email) { this(null, name, email); }
    public Long getId() { return id; }
}
enum UserStatus { ACTIVE, INACTIVE }
record UserResponse(Long id, String name, String email, String status, java.time.LocalDateTime createdAt) {}
record CreateUserRequest(String name, String email, String password, int age) {}
class ResourceNotFoundException extends RuntimeException {
    ResourceNotFoundException(String r, Object id) { super(r + " not found: " + id); }
}
