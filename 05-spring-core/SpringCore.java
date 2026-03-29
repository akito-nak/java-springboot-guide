package spring.core;

// ============================================================
// 05 - SPRING CORE: IoC & Dependency Injection
// ============================================================
// Spring is fundamentally about one idea: Inversion of Control (IoC).
//
// Without Spring, YOU create objects and wire them together:
//   UserRepository repo = new PostgresUserRepository(dataSource);
//   EmailService email = new SmtpEmailService(smtpConfig);
//   UserService service = new UserService(repo, email);
//
// With Spring, you DECLARE what you need; Spring creates and wires it:
//   @Service class UserService { // Spring sees this
//     @Autowired UserRepository repo;  // Spring injects this
//     @Autowired EmailService email;   // and this
//   }
//
// Why does this matter?
//   ✅ Testable — swap real implementations for mocks in tests
//   ✅ Decoupled — classes don't know how their dependencies are created
//   ✅ Configurable — change implementations without changing code
//   ✅ Centrally managed — Spring handles lifecycle (creation, destruction)

import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

// ─────────────────────────────────────────────────────────────
// STEP 1: Define interfaces (contracts)
// ─────────────────────────────────────────────────────────────

interface UserRepository {
    User findById(Long id);
    User save(User user);
}

interface EmailService {
    void sendWelcomeEmail(String to, String name);
}

interface NotificationService {
    void notify(String userId, String message);
}

// Simple domain object
record User(Long id, String name, String email) {}

// ─────────────────────────────────────────────────────────────
// STEP 2: Implement them, annotated as Spring beans
// ─────────────────────────────────────────────────────────────
// Spring scans for these annotations and manages the objects:
//
// @Component     — generic bean (use for anything that doesn't fit below)
// @Service       — business logic layer
// @Repository    — data access layer (also handles DB exceptions)
// @Controller    — web layer (handles HTTP requests)
// @RestController — @Controller + @ResponseBody

@Repository
class JpaUserRepository implements UserRepository {
    // In real life, this would use EntityManager or extend JpaRepository
    private final java.util.Map<Long, User> store = new java.util.HashMap<>();

    @Override
    public User findById(Long id) {
        return store.get(id);
    }

    @Override
    public User save(User user) {
        store.put(user.id(), user);
        return user;
    }
}

@Component
class SmtpEmailService implements EmailService {
    // In real life, this would use JavaMailSender
    @Override
    public void sendWelcomeEmail(String to, String name) {
        System.out.printf("📧 Sending welcome email to %s <%s>%n", name, to);
    }
}

// ─────────────────────────────────────────────────────────────
// STEP 3: Wire dependencies with @Autowired
// ─────────────────────────────────────────────────────────────
// Spring sees @Service, creates ONE instance (singleton by default),
// and injects the dependencies declared in the constructor.

@Service
class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;

    // Constructor injection — the PREFERRED approach
    // Why? Dependencies are explicit, immutable (final), and the class
    // is easily testable (just pass mocks to the constructor).
    // Spring 4.3+: @Autowired is optional when there's only one constructor.
    @Autowired
    public UserService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public User registerUser(String name, String email) {
        User user = new User(System.currentTimeMillis(), name, email);
        User saved = userRepository.save(user);
        emailService.sendWelcomeEmail(email, name);
        return saved;
    }

    public User getUser(Long id) {
        User user = userRepository.findById(id);
        if (user == null) throw new RuntimeException("User not found: " + id);
        return user;
    }
}

// ─────────────────────────────────────────────────────────────
// THREE WAYS TO INJECT (know them all, use constructor injection)
// ─────────────────────────────────────────────────────────────

// CONSTRUCTOR INJECTION ✅ (preferred)
@Service
class ConstructorInjectionExample {
    private final UserRepository repo;  // final = immutable after construction
    private final EmailService email;

    public ConstructorInjectionExample(UserRepository repo, EmailService email) {
        this.repo = repo;
        this.email = email;
    }
}

// FIELD INJECTION ⚠️ (convenient, but bad for testing)
@Service
class FieldInjectionExample {
    @Autowired private UserRepository repo;   // Spring injects via reflection
    @Autowired private EmailService email;    // Can't easily test without Spring context
}

// SETTER INJECTION (for optional dependencies)
@Service
class SetterInjectionExample {
    private UserRepository repo;
    private NotificationService notifications; // optional

    @Autowired
    public void setRepo(UserRepository repo) { this.repo = repo; }

    @Autowired(required = false)  // won't fail if no bean exists
    public void setNotifications(NotificationService ns) { this.notifications = ns; }
}

// ─────────────────────────────────────────────────────────────
// BEAN SCOPES
// ─────────────────────────────────────────────────────────────
// Singleton (default) — one instance per Spring context (shared)
// Prototype           — new instance every time it's requested
// Request             — new instance per HTTP request (web only)
// Session             — new instance per HTTP session (web only)

@Component
// @Scope("singleton") // implicit default
class SingletonBean {
    private int counter = 0;
    public int increment() { return ++counter; }
    // All classes sharing this bean see the same counter
}

@Component
@Scope("prototype")
class PrototypeBean {
    private int counter = 0;
    public int increment() { return ++counter; }
    // Each injection gets a fresh instance — counters are independent
}

// ─────────────────────────────────────────────────────────────
// @Configuration AND @Bean
// ─────────────────────────────────────────────────────────────
// When you can't annotate a class (third-party library, complex setup),
// define beans manually in a @Configuration class.

@Configuration
class AppConfig {

    // A @Bean method = "hey Spring, manage this object"
    // The method name becomes the bean name by default
    @Bean
    public EmailService emailService() {
        return new SmtpEmailService();
    }

    // Beans can depend on other beans — Spring handles the ordering
    @Bean
    public UserService userService(UserRepository repo, EmailService emailService) {
        return new UserService(repo, emailService);
    }

    // Conditional beans — only register if a condition is met
    @Bean
    @ConditionalOnProperty(name = "feature.notifications.enabled", havingValue = "true")
    public NotificationService notificationService() {
        return (userId, message) ->
            System.out.println("Notifying " + userId + ": " + message);
    }
}

// ─────────────────────────────────────────────────────────────
// QUALIFIERS — when you have multiple implementations
// ─────────────────────────────────────────────────────────────

interface PaymentProcessor {
    String process(double amount);
}

@Component
@Qualifier("stripe")
class StripePaymentProcessor implements PaymentProcessor {
    @Override
    public String process(double amount) { return "Processed $" + amount + " via Stripe"; }
}

@Component
@Qualifier("paypal")
class PayPalPaymentProcessor implements PaymentProcessor {
    @Override
    public String process(double amount) { return "Processed $" + amount + " via PayPal"; }
}

@Service
class OrderService {
    private final PaymentProcessor processor;

    public OrderService(@Qualifier("stripe") PaymentProcessor processor) {
        this.processor = processor;
    }
}

// ─────────────────────────────────────────────────────────────
// @Primary — default when qualifier not specified
// ─────────────────────────────────────────────────────────────

@Component
@Primary // this one is used when @Qualifier isn't specified
class DefaultPaymentProcessor implements PaymentProcessor {
    @Override
    public String process(double amount) { return "Default payment processing"; }
}

// ─────────────────────────────────────────────────────────────
// @Value — inject configuration values
// ─────────────────────────────────────────────────────────────

@Service
class ConfigurableService {
    @Value("${app.name:MyApp}")       // property name, with default
    private String appName;

    @Value("${app.timeout:30}")
    private int timeout;

    @Value("${app.features:[]}")
    private List<String> features;

    @Value("#{systemProperties['java.version']}") // Spring Expression Language (SpEL)
    private String javaVersion;
}

// ─────────────────────────────────────────────────────────────
// BEAN LIFECYCLE
// ─────────────────────────────────────────────────────────────

@Component
class LifecycleBean {
    // Called AFTER Spring has injected all dependencies
    @PostConstruct
    public void init() {
        System.out.println("Bean is ready! Dependencies are all injected.");
    }

    // Called BEFORE Spring destroys the bean (app shutdown)
    @PreDestroy
    public void cleanup() {
        System.out.println("Bean is being destroyed. Releasing resources.");
    }
}

// Same thing via @Bean attributes:
@Configuration
class LifecycleConfig {
    @Bean(initMethod = "init", destroyMethod = "cleanup")
    public SomeResource someResource() {
        return new SomeResource();
    }

    static class SomeResource {
        public void init() { System.out.println("Resource initialised"); }
        public void cleanup() { System.out.println("Resource cleaned up"); }
    }
}
