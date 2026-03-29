package springboot;

// ============================================================
// 06 - SPRING BOOT
// ============================================================
// Spring Boot is opinionated Spring. It provides:
//   ✅ Auto-configuration — sensible defaults, no XML
//   ✅ Embedded server    — run as a standalone JAR
//   ✅ Starter POMs       — dependency bundles that just work
//   ✅ Actuator           — production metrics/health out of the box
//   ✅ DevTools           — hot reload during development
//
// The magic of @SpringBootApplication:

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.*;
import jakarta.annotation.*;

// ─────────────────────────────────────────────────────────────
// MAIN APPLICATION CLASS
// ─────────────────────────────────────────────────────────────

@SpringBootApplication
// @SpringBootApplication is a shortcut for:
//   @Configuration          — this is a config class
//   @EnableAutoConfiguration — let Spring Boot configure things automatically
//   @ComponentScan          — scan this package and below for @Component etc.
@EnableConfigurationProperties(AppProperties.class)
public class Application {

    public static void main(String[] args) {
        // Creates the ApplicationContext, starts the embedded Tomcat,
        // scans components, wires dependencies — all from this one line
        SpringApplication.run(Application.class, args);
    }
}

// ─────────────────────────────────────────────────────────────
// TYPE-SAFE CONFIGURATION WITH @ConfigurationProperties
// ─────────────────────────────────────────────────────────────
// Instead of scattering @Value everywhere, bind all related properties
// to a single POJO. This is the clean, scalable approach.
//
// application.yml:
//   app:
//     name: My Awesome App
//     description: Does awesome things
//     version: 1.0.0
//     email:
//       from: noreply@example.com
//       host: smtp.example.com
//       port: 587
//     rate-limiting:
//       enabled: true
//       requests-per-minute: 100

@ConfigurationProperties(prefix = "app")
class AppProperties {
    private String name;
    private String description;
    private String version;
    private Email email = new Email();
    private RateLimiting rateLimiting = new RateLimiting();

    // Getters and setters (or use records with @ConstructorBinding)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    // ... (omitted for brevity)

    public record Email(String from, String host, int port) {
        public Email() { this("", "", 587); }
    }

    public record RateLimiting(boolean enabled, int requestsPerMinute) {
        public RateLimiting() { this(false, 60); }
    }
}

// ─────────────────────────────────────────────────────────────
// PROFILES — different config for different environments
// ─────────────────────────────────────────────────────────────
// Activate with: --spring.profiles.active=production
// Or in application.yml: spring.profiles.active: production
// Or via env var: SPRING_PROFILES_ACTIVE=production

interface DatabaseConfig {
    String getUrl();
    String getUsername();
}

@Component
@Profile("development")   // only active in dev profile
class H2DatabaseConfig implements DatabaseConfig {
    @Override public String getUrl() { return "jdbc:h2:mem:devdb"; }
    @Override public String getUsername() { return "sa"; }
}

@Component
@Profile("production")    // only active in prod profile
class PostgresDatabaseConfig implements DatabaseConfig {
    @Override public String getUrl() { return "jdbc:postgresql://prod-server:5432/appdb"; }
    @Override public String getUsername() { return "${DB_USERNAME}"; } // from env var
}

@Component
@Profile({"staging", "production"})  // multiple profiles
class AuditingService {
    @PostConstruct
    public void init() { System.out.println("Auditing enabled (staging/prod only)"); }
}

// ─────────────────────────────────────────────────────────────
// APPLICATION PROPERTIES STRUCTURE
// ─────────────────────────────────────────────────────────────
// Spring Boot reads properties from (in order of precedence):
//   1. application.properties or application.yml (in src/main/resources)
//   2. application-{profile}.yml (profile-specific overrides)
//   3. Environment variables
//   4. Command-line arguments (highest priority)

/*
// application.yml (the canonical Spring Boot config file):

spring:
  application:
    name: my-app

  # Database
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
      connection-timeout: 30000

  # JPA / Hibernate
  jpa:
    hibernate:
      ddl-auto: validate          # validate | update | create | create-drop
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  # Connection pool

# Server
server:
  port: 8080
  servlet:
    context-path: /api

# Logging
logging:
  level:
    root: INFO
    com.myapp: DEBUG
    org.hibernate.SQL: DEBUG
  pattern:
    console: "%d{HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized

# Custom properties
app:
  name: My App
  version: 1.0.0
*/

// ─────────────────────────────────────────────────────────────
// SPRING BOOT ACTUATOR
// ─────────────────────────────────────────────────────────────
// Add: spring-boot-starter-actuator to pom.xml / build.gradle
// Provides built-in endpoints:
//   GET /actuator/health    — app health (up/down)
//   GET /actuator/info      — app info (version, build)
//   GET /actuator/metrics   — JVM, HTTP, custom metrics
//   GET /actuator/env       — environment properties
//   GET /actuator/beans     — all beans in context
//   GET /actuator/mappings  — all @RequestMapping endpoints
//   POST /actuator/shutdown — graceful shutdown

// Custom health indicator
import org.springframework.boot.actuate.health.*;

@Component
class DatabaseHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // Perform your health check
        boolean dbIsUp = checkDatabase();
        if (dbIsUp) {
            return Health.up()
                .withDetail("database", "PostgreSQL")
                .withDetail("responseTime", "5ms")
                .build();
        } else {
            return Health.down()
                .withDetail("error", "Cannot connect to database")
                .build();
        }
    }

    private boolean checkDatabase() {
        // In reality, try to execute a simple query
        return true;
    }
}

// Custom info contributor
import org.springframework.boot.actuate.info.*;

@Component
class AppInfoContributor implements InfoContributor {
    private final AppProperties props;

    AppInfoContributor(AppProperties props) { this.props = props; }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("app", java.util.Map.of(
            "name", props.getName(),
            "version", props.getVersion()
        ));
    }
}

// Custom Micrometer metric
import io.micrometer.core.instrument.*;

@Service
class MetricsService {
    private final Counter ordersProcessed;
    private final Timer orderProcessingTime;

    MetricsService(MeterRegistry registry) {
        this.ordersProcessed = Counter.builder("orders.processed")
            .description("Number of orders processed")
            .tag("environment", "production")
            .register(registry);

        this.orderProcessingTime = Timer.builder("orders.processing.time")
            .description("Time to process an order")
            .register(registry);
    }

    public void processOrder(Runnable work) {
        orderProcessingTime.record(work);
        ordersProcessed.increment();
    }
}
