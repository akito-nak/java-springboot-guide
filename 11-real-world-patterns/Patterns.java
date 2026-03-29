package patterns;

// ============================================================
// 11 - REAL-WORLD PATTERNS & ADVANCED SPRING
// ============================================================
// These are the patterns that separate "it works locally" code
// from production-grade applications. Every senior Java/Spring
// developer is expected to know these cold.

import org.springframework.stereotype.*;
import org.springframework.context.annotation.*;
import org.springframework.cache.annotation.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.context.*;
import org.springframework.transaction.annotation.*;
import org.springframework.aop.aspectj.annotation.*;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.*;
import java.util.*;
import java.time.*;

// ─────────────────────────────────────────────────────────────
// EXCEPTION HIERARCHY — structured error handling
// ─────────────────────────────────────────────────────────────
// Build a hierarchy of exceptions that map to HTTP status codes.
// Your @ControllerAdvice handler maps each type to a response.

abstract class AppException extends RuntimeException {
    private final String errorCode;

    protected AppException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}

// 4xx — client errors
class NotFoundException       extends AppException {
    NotFoundException(String resource, Object id) {
        super(resource + " not found with id: " + id, "NOT_FOUND");
    }
}

class ValidationException     extends AppException {
    private final Map<String, String> fieldErrors;
    ValidationException(Map<String, String> errors) {
        super("Validation failed", "VALIDATION_ERROR");
        this.fieldErrors = errors;
    }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
}

class ConflictException        extends AppException {
    ConflictException(String message) { super(message, "CONFLICT"); }
}

class UnauthorizedException    extends AppException {
    UnauthorizedException(String message) { super(message, "UNAUTHORIZED"); }
}

class ForbiddenException       extends AppException {
    ForbiddenException(String message) { super(message, "FORBIDDEN"); }
}

// 5xx — server/infrastructure errors
class ExternalServiceException extends AppException {
    ExternalServiceException(String service, Throwable cause) {
        super("External service failed: " + service, "EXTERNAL_SERVICE_ERROR");
    }
}

// ─────────────────────────────────────────────────────────────
// AOP (ASPECT-ORIENTED PROGRAMMING)
// ─────────────────────────────────────────────────────────────
// Cross-cutting concerns (logging, timing, auditing) without
// polluting every service method with boilerplate.
//
// Spring's @Transactional and @Cacheable are implemented as aspects!
//
// Pointcut syntax:
//   execution(* com.example.service.*.*(..))
//     ↑ any return type, any class in service package, any method, any args

@Aspect
@Component
class LoggingAspect {

    // Execute AROUND every service method
    @Around("execution(* com.example.service.*.*(..))")
    public Object logMethodCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        long start = System.currentTimeMillis();

        System.out.printf("[LOG] → %s(%s)%n", method, Arrays.toString(args));

        try {
            Object result = joinPoint.proceed(); // call the actual method
            long elapsed = System.currentTimeMillis() - start;
            System.out.printf("[LOG] ← %s returned in %dms%n", method, elapsed);
            return result;
        } catch (Exception ex) {
            System.out.printf("[LOG] ✗ %s threw %s: %s%n",
                method, ex.getClass().getSimpleName(), ex.getMessage());
            throw ex; // re-throw — don't swallow
        }
    }

    // Before — runs before the method, can't stop it (use @Around for that)
    @Before("execution(* com.example.service.*.delete*(..))")
    public void logDeletion(JoinPoint joinPoint) {
        System.out.println("[AUDIT] Deletion called: " + joinPoint.getSignature().getName());
    }

    // After returning — runs after successful return
    @AfterReturning(pointcut = "execution(* com.example.service.UserService.create*(..))",
                    returning = "result")
    public void logCreation(JoinPoint joinPoint, Object result) {
        System.out.println("[AUDIT] Created: " + result);
    }
}

// Custom annotation + AOP = reusable cross-cutting concerns
@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@interface RateLimit {
    int requestsPerMinute() default 60;
    String key() default ""; // SpEL expression for the rate limit key
}

@Aspect
@Component
class RateLimitAspect {
    private final Map<String, java.util.Deque<Instant>> requestLog = new HashMap<>();

    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = joinPoint.getSignature().toShortString();
        Instant now = Instant.now();
        Instant oneMinuteAgo = now.minus(Duration.ofMinutes(1));

        requestLog.computeIfAbsent(key, k -> new java.ArrayDeque<>());
        java.util.Deque<Instant> requests = requestLog.get(key);

        // Remove requests older than 1 minute
        while (!requests.isEmpty() && requests.peekFirst().isBefore(oneMinuteAgo)) {
            requests.pollFirst();
        }

        if (requests.size() >= rateLimit.requestsPerMinute()) {
            throw new RuntimeException("Rate limit exceeded for " + key);
        }

        requests.addLast(now);
        return joinPoint.proceed();
    }
}

// Usage:
@Service
class SearchService {
    @RateLimit(requestsPerMinute = 100)
    public List<String> search(String query) {
        return List.of("result1", "result2");
    }
}

// ─────────────────────────────────────────────────────────────
// CACHING — don't compute the same thing twice
// ─────────────────────────────────────────────────────────────
// Add: spring-boot-starter-cache
// Add: com.github.ben-manes.caffeine:caffeine (for in-process cache)
//
// application.yml:
//   spring.cache.type: caffeine
//   spring.cache.caffeine.spec: maximumSize=500,expireAfterWrite=10m

@Service
@EnableCaching  // on your @SpringBootApplication or @Configuration
class ProductService {

    // @Cacheable — cache the return value, keyed on parameters
    // Next call with same id returns cached value WITHOUT executing method body
    @Cacheable(value = "products", key = "#id")
    public Object findById(Long id) {
        System.out.println("Loading product " + id + " from DB..."); // only on cache miss
        return "Product " + id;
    }

    // @Cacheable with condition
    @Cacheable(value = "products", key = "#id", unless = "#result == null")
    public Object findByIdOrNull(Long id) { return null; }

    // @CachePut — always execute method AND update cache
    @CachePut(value = "products", key = "#product.id")
    public Object updateProduct(Object product) {
        return product; // cache updated with new value
    }

    // @CacheEvict — remove from cache
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        System.out.println("Deleted product " + id);
    }

    // Evict ALL entries in a cache
    @CacheEvict(value = "products", allEntries = true)
    @Scheduled(cron = "0 0 * * * *") // every hour
    public void evictProductCache() {
        System.out.println("Product cache cleared");
    }
}

// ─────────────────────────────────────────────────────────────
// EVENTS — decouple side effects from core logic
// ─────────────────────────────────────────────────────────────
// Instead of directly calling EmailService from UserService,
// publish an event. Multiple listeners can react independently.
// Keeps UserService focused on its core responsibility.

// Define the event
record UserRegisteredEvent(Long userId, String email, String name) {}

// Publish the event from your service
@Service
class UserService {
    private final ApplicationEventPublisher eventPublisher;

    UserService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void registerUser(String name, String email) {
        // ... save user to DB ...
        Long userId = 42L; // from DB

        // Publish event — UserService no longer depends on Email, Notifications, Analytics...
        eventPublisher.publishEvent(new UserRegisteredEvent(userId, email, name));
        System.out.println("User registered: " + name);
    }
}

// Multiple listeners can react to the same event independently
@Component
class WelcomeEmailListener {
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        System.out.println("Sending welcome email to " + event.email());
    }
}

@Component
class UserAnalyticsListener {
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        System.out.println("Tracking registration in analytics: " + event.userId());
    }
}

@Component
class AsyncNotificationListener {
    @EventListener
    @Async  // executes in a separate thread — won't slow down the main flow
    public void onUserRegistered(UserRegisteredEvent event) {
        System.out.println("Sending push notification (async) to " + event.userId());
    }
}

// ─────────────────────────────────────────────────────────────
// ASYNC PROCESSING
// ─────────────────────────────────────────────────────────────
// @Async makes a method execute in a thread pool asynchronously.
// The caller gets a CompletableFuture immediately.

@Service
@EnableAsync  // on your @SpringBootApplication or @Configuration
class ReportService {

    @Async
    public java.util.concurrent.CompletableFuture<String> generateReport(String type) {
        System.out.println("Generating " + type + " report on thread: " + Thread.currentThread().getName());
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return java.util.concurrent.CompletableFuture.completedFuture("Report: " + type);
    }
}

// Custom async thread pool configuration
@Configuration
class AsyncConfig {
    @Bean("reportExecutor")
    public java.util.concurrent.Executor reportTaskExecutor() {
        var executor = new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("report-");
        executor.initialize();
        return executor;
    }
}

// ─────────────────────────────────────────────────────────────
// SCHEDULED TASKS
// ─────────────────────────────────────────────────────────────

@Component
@EnableScheduling  // on @SpringBootApplication
class ScheduledTasks {

    // Fixed rate — every 5 seconds, regardless of completion time
    @Scheduled(fixedRate = 5000)
    public void heartbeat() {
        System.out.println("Heartbeat: " + LocalDateTime.now());
    }

    // Fixed delay — 5 seconds AFTER previous execution completes
    @Scheduled(fixedDelay = 5000)
    public void processQueue() {
        System.out.println("Processing queue...");
    }

    // Cron expression: second minute hour day-of-month month day-of-week
    @Scheduled(cron = "0 0 2 * * *")   // every day at 2 AM
    public void dailyCleanup() {
        System.out.println("Running daily cleanup...");
    }

    @Scheduled(cron = "0 0 9 * * MON-FRI") // weekdays at 9 AM
    public void sendDailyDigest() {
        System.out.println("Sending daily digest...");
    }
}

// ─────────────────────────────────────────────────────────────
// BUILDER PATTERN — for complex object construction
// ─────────────────────────────────────────────────────────────

class EmailMessage {
    private final String from;
    private final List<String> to;
    private final String subject;
    private final String body;
    private final boolean html;
    private final List<String> attachments;

    private EmailMessage(Builder builder) {
        this.from = builder.from;
        this.to = List.copyOf(builder.to);
        this.subject = builder.subject;
        this.body = builder.body;
        this.html = builder.html;
        this.attachments = List.copyOf(builder.attachments);
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String from = "noreply@example.com";
        private final List<String> to = new ArrayList<>();
        private String subject;
        private String body;
        private boolean html = false;
        private final List<String> attachments = new ArrayList<>();

        public Builder from(String from)           { this.from = from; return this; }
        public Builder to(String... emails)        { this.to.addAll(Arrays.asList(emails)); return this; }
        public Builder subject(String subject)     { this.subject = subject; return this; }
        public Builder body(String body)           { this.body = body; return this; }
        public Builder html()                      { this.html = true; return this; }
        public Builder attach(String path)         { this.attachments.add(path); return this; }

        public EmailMessage build() {
            Objects.requireNonNull(subject, "Subject is required");
            Objects.requireNonNull(body, "Body is required");
            if (to.isEmpty()) throw new IllegalStateException("At least one recipient required");
            return new EmailMessage(this);
        }
    }
}

// Usage — reads like a sentence:
class EmailDemo {
    static void demo() {
        EmailMessage email = EmailMessage.builder()
            .to("alice@example.com", "bob@example.com")
            .subject("Your order has shipped!")
            .body("<h1>Great news!</h1><p>Your order is on its way.</p>")
            .html()
            .attach("/tmp/invoice.pdf")
            .build();
    }
}

// ─────────────────────────────────────────────────────────────
// STRATEGY PATTERN — swap algorithms at runtime
// ─────────────────────────────────────────────────────────────

interface DiscountStrategy {
    double apply(double price);
    String getCode();
}

@Component("noDiscount")
class NoDiscount implements DiscountStrategy {
    @Override public double apply(double price) { return price; }
    @Override public String getCode() { return "NONE"; }
}

@Component("percentageDiscount")
class PercentageDiscount implements DiscountStrategy {
    private final double percentage = 0.10;
    @Override public double apply(double price) { return price * (1 - percentage); }
    @Override public String getCode() { return "PERCENT10"; }
}

@Component("flatDiscount")
class FlatDiscount implements DiscountStrategy {
    @Override public double apply(double price) { return Math.max(0, price - 20); }
    @Override public String getCode() { return "FLAT20"; }
}

@Service
class PricingService {
    // Spring injects ALL implementations of DiscountStrategy into this map!
    // Map key = bean name, Map value = bean instance
    private final Map<String, DiscountStrategy> strategies;

    PricingService(Map<String, DiscountStrategy> strategies) {
        this.strategies = strategies;
    }

    public double calculatePrice(double basePrice, String couponCode) {
        DiscountStrategy strategy = strategies.entrySet().stream()
            .filter(e -> e.getValue().getCode().equals(couponCode))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(strategies.get("noDiscount"));

        return strategy.apply(basePrice);
    }
}

// ─────────────────────────────────────────────────────────────
// CIRCUIT BREAKER PATTERN — resilience for external calls
// ─────────────────────────────────────────────────────────────
// Add: io.github.resilience4j:resilience4j-spring-boot3
// application.yml:
//   resilience4j.circuitbreaker.instances.paymentService:
//     slidingWindowSize: 10
//     failureRateThreshold: 50
//     waitDurationInOpenState: 30s

@Service
class PaymentServiceClient {
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(
        name = "paymentService",
        fallbackMethod = "paymentFallback"
    )
    @io.github.resilience4j.retry.annotation.Retry(name = "paymentService")
    public String processPayment(double amount) {
        // Call external payment service — might fail!
        throw new RuntimeException("Payment service is down");
    }

    // Called when circuit is open or retries exhausted
    private String paymentFallback(double amount, Exception e) {
        System.err.println("Payment service unavailable, queuing payment for later");
        return "QUEUED";
    }
}
