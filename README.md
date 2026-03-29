# Java + Spring Boot: The Complete Guide
### From Zero to Production-Grade Applications

> This isn't your average boring tutorial. We're going to learn Java and Spring Boot the way working engineers actually use them — with real context, real patterns, and real explanations of *why* things work the way they do, not just *how*.
>
> Whether you're just starting out or you've been writing Java for years and want a solid reference, this guide has you covered.

---

## Table of Contents

1. [Repo Structure](#repo-structure)
2. [Prerequisites & Setup](#prerequisites--setup)
3. [Chapter 1 — Java Fundamentals](#chapter-1--java-fundamentals)
4. [Chapter 2 — Object-Oriented Programming](#chapter-2--object-oriented-programming)
5. [Chapter 3 — Collections & Streams](#chapter-3--collections--streams)
6. [Chapter 4 — Modern Java (8–21)](#chapter-4--modern-java-8-21)
7. [Chapter 5 — Spring Core: IoC & DI](#chapter-5--spring-core-ioc--dependency-injection)
8. [Chapter 6 — Spring Boot](#chapter-6--spring-boot)
9. [Chapter 7 — Spring Data JPA](#chapter-7--spring-data-jpa)
10. [Chapter 8 — Spring Security](#chapter-8--spring-security)
11. [Chapter 9 — Building REST APIs](#chapter-9--building-rest-apis)
12. [Chapter 10 — Testing](#chapter-10--testing)
13. [Chapter 11 — Real-World Patterns](#chapter-11--real-world-patterns)
14. [Chapter 12 — Production Readiness](#chapter-12--production-readiness)
15. [Quick Reference & Interview Prep](#quick-reference--interview-prep)

---

## Repo Structure

```
java-springboot-guide/
│
├── 01-java-fundamentals/
│   └── Fundamentals.java       ← Primitives, operators, control flow, strings, arrays
│
├── 02-oop/
│   └── OOP.java                ← Classes, inheritance, interfaces, polymorphism, enums
│
├── 03-collections-streams/
│   └── CollectionsAndStreams.java  ← List/Map/Set, Streams, Collectors, Optional
│
├── 04-modern-java/
│   └── ModernJava.java         ← Lambdas, records, sealed classes, pattern matching, Date/Time
│
├── 05-spring-core/
│   └── SpringCore.java         ← IoC, @Component/@Service/@Repository, @Autowired, scopes
│
├── 06-spring-boot/
│   └── SpringBoot.java         ← @SpringBootApplication, profiles, ConfigurationProperties, Actuator
│
├── 07-spring-data/
│   └── SpringData.java         ← @Entity, JpaRepository, derived queries, @Query, pagination, N+1
│
├── 08-spring-security/
│   └── SpringSecurity.java     ← SecurityFilterChain, JWT, @PreAuthorize, BCrypt
│
├── 09-spring-rest/
│   └── SpringRest.java         ← @RestController, request handling, validation, exception handling
│
├── 10-testing/
│   └── Testing.java            ← JUnit 5, Mockito, @WebMvcTest, @DataJpaTest, Testcontainers
│
├── 11-real-world-patterns/
│   └── Patterns.java           ← AOP, caching, events, async, Builder, Strategy, Circuit Breaker
│
├── pom.xml                     ← Maven build file with all dependencies annotated
└── README.md                   ← This file — the complete tutorial
```

---

## Prerequisites & Setup

### What You Need

- **JDK 21** (recommended) — download from [adoptium.net](https://adoptium.net) or use `sdk install java 21-tem` via [SDKMAN](https://sdkman.io)
- **Maven 3.9+** or **Gradle 8+** — Maven comes bundled with most IDEs
- **IntelliJ IDEA** (Community is free; Ultimate adds Spring support) or **VS Code** with the Java Extension Pack

### Quick Start: Create a Spring Boot Project

The fastest way is [start.spring.io](https://start.spring.io). Select:
- Project: Maven
- Language: Java
- Spring Boot: 3.3.x
- Java: 21
- Dependencies: Web, JPA, Security, Validation, Actuator, DevTools, Lombok

Then unzip and open in IntelliJ. That's it. Spring Boot handles the rest.

### Running the Examples in This Repo

```bash
# Compile everything
mvn compile

# Run a specific class
mvn exec:java -Dexec.mainClass="fundamentals.Fundamentals"

# Run all tests
mvn test

# Build runnable JAR
mvn package
java -jar target/java-springboot-guide-1.0.0.jar
```

---

## Chapter 1 — Java Fundamentals

> **File:** `01-java-fundamentals/Fundamentals.java`

### Java's Type System in 60 Seconds

Java is **statically typed** — every variable has a type that's known at compile time. Every **primitive** type is stored directly in memory. Every **reference type** is stored as a pointer to an object on the heap.

```
Primitives (8 total):
  byte (8-bit) │ short (16-bit) │ int (32-bit) │ long (64-bit)
  float (32-bit) │ double (64-bit) │ boolean │ char (16-bit Unicode)

Reference types: everything else (String, arrays, your classes, etc.)
```

### The Types You'll Actually Use

```java
int     i = 42;           // ← everyday integer
long    l = 42L;          // ← big numbers, timestamps; note the L
double  d = 3.14;         // ← default floating-point
boolean b = true;
String  s = "hello";      // ← not a primitive! String is a class
```

### The Pitfalls Every Java Dev Hits

**1. Integer overflow — Java wraps silently**
```java
int max = Integer.MAX_VALUE; // 2,147,483,647
System.out.println(max + 1); // prints -2147483648. No error!
```
Use `long` for large numbers, or `Math.addExact()` if you want an exception.

**2. `==` vs `.equals()` — the eternal trap**
```java
String a = new String("hello");
String b = new String("hello");
System.out.println(a == b);       // false! (different objects on the heap)
System.out.println(a.equals(b));  // true  ← ALWAYS use .equals() for objects
```
`==` compares *memory addresses* (references). `.equals()` compares *content*.
This applies to Strings, Integer objects, everything non-primitive.

**3. NullPointerException — the billion-dollar mistake**
```java
String name = null;
name.toUpperCase(); // 💥 NullPointerException
```
Always check for null, or better yet, use `Optional<T>` (covered in Chapter 4).

**4. Floating-point is approximate**
```java
System.out.println(0.1 + 0.2); // 0.30000000000000004
```
For money or precision, always use `BigDecimal`. Never `double`.
```java
BigDecimal price = new BigDecimal("9.99");
BigDecimal tax   = new BigDecimal("0.08");
BigDecimal total = price.multiply(tax.add(BigDecimal.ONE)); // 10.7892
```

### Strings — Your Best Friend With Caveats

Strings in Java are **immutable** — once created, they can't change. Every operation that seems to "modify" a string actually creates a new one.

```java
String s = "hello";
s.toUpperCase(); // does NOTHING to s
String upper = s.toUpperCase(); // creates a NEW string

// BAD: builds 100 new String objects
String result = "";
for (int i = 0; i < 100; i++) result += "item" + i;

// GOOD: one buffer, appends in place
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 100; i++) sb.append("item").append(i);
String result2 = sb.toString();
```

### The Modern `switch` Expression (Java 14+)

Old switch was error-prone because of fall-through. New switch is an expression:

```java
// Old (error-prone)
String type;
switch (day) {
    case "MON": case "TUE": type = "Weekday"; break;  // forget break = bug!
    default: type = "Weekend";
}

// New (safe, returns a value)
String type = switch (day) {
    case "MON", "TUE", "WED", "THU", "FRI" -> "Weekday";
    case "SAT", "SUN" -> "Weekend";
    default -> throw new IllegalArgumentException("Unknown: " + day);
};
```

---

## Chapter 2 — Object-Oriented Programming

> **File:** `02-oop/OOP.java`

### The Four Pillars (Actually Useful Explanation)

**1. Encapsulation** — hide your data, expose your behaviour.
Don't let other classes reach into your object and mess with its internal state. Make fields `private`, validate in setters or constructors.

```java
class BankAccount {
    private double balance; // nobody touches this directly

    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Must be positive");
        balance += amount; // validated and controlled
    }
}
```

**2. Inheritance** — reuse and extend existing classes.
Use it when there's a genuine "is-a" relationship. A `SavingsAccount` IS-A `BankAccount`.

```java
class SavingsAccount extends BankAccount {
    @Override
    public void withdraw(double amount) {
        if (getBalance() - amount < 100) throw new IllegalStateException("Minimum balance!");
        super.withdraw(amount); // call parent logic
    }
}
```

⚠️ **Inheritance is often overused.** Favour composition ("has-a") over inheritance ("is-a") when in doubt. A `Car` HAS-A `Engine` — it shouldn't extend `Engine`.

**3. Polymorphism** — one interface, many shapes.
Code to the abstract type, not the concrete implementation. This is where Java's power lives.

```java
List<Shape> shapes = List.of(new Circle(5), new Rectangle(3, 4), new Triangle(6, 8));
shapes.forEach(s -> System.out.println(s.area())); // each shape knows its own area formula
```

The JVM picks the right `area()` method at runtime. Your code doesn't care which subtype it is.

**4. Abstraction** — hide complexity behind a clean interface.
An abstract class provides a partial implementation + forces subclasses to fill the gaps.

### `interface` vs `abstract class` — When to Use Each

| | `interface` | `abstract class` |
|---|---|---|
| Multiple inheritance | ✅ (class can implement many) | ❌ (can only extend one) |
| Has state (fields) | ❌ (constants only) | ✅ |
| Has concrete methods | ✅ (default methods) | ✅ |
| Constructor | ❌ | ✅ |
| When to use | Define a *capability* or *role* (`Printable`, `Serializable`) | Define a *partial base implementation* (`Shape`, `AbstractRepository`) |

### Enums Are More Powerful Than You Think

Java enums are classes. They can have fields, constructors, and methods:

```java
enum Planet {
    MERCURY(3.303e+23, 2.4397e6),
    EARTH(5.976e+24, 6.37814e6);

    private final double mass;
    private final double radius;

    Planet(double mass, double radius) { this.mass = mass; this.radius = radius; }

    double surfaceGravity() { return 6.67300E-11 * mass / (radius * radius); }
}

double weight = Planet.EARTH.surfaceGravity() * 75; // weight on Earth
```

### `instanceof` with Pattern Matching (Java 16+)

Old, verbose way:
```java
if (shape instanceof Circle) {
    Circle c = (Circle) shape; // redundant cast!
    System.out.println(c.radius());
}
```

New, clean way:
```java
if (shape instanceof Circle c) {
    System.out.println(c.radius()); // c is already Circle, no cast needed
}
```

---

## Chapter 3 — Collections & Streams

> **File:** `03-collections-streams/CollectionsAndStreams.java`

### Pick the Right Collection — It Matters

| What you need | Use | Why |
|---|---|---|
| Ordered list, fast index access | `ArrayList` | O(1) get, O(n) insert in middle |
| Ordered list, lots of inserts/removes | `LinkedList` | O(1) insert at head/tail |
| No duplicates, don't care about order | `HashSet` | O(1) add/contains |
| No duplicates, insertion order | `LinkedHashSet` | O(1) ops + ordered |
| No duplicates, sorted | `TreeSet` | O(log n), naturally sorted |
| Key-value pairs, fast lookup | `HashMap` | O(1) get/put |
| Key-value, insertion order | `LinkedHashMap` | O(1) ops + ordered |
| Key-value, sorted by key | `TreeMap` | O(log n), naturally sorted |
| FIFO queue | `ArrayDeque` | O(1) offer/poll |
| Priority-based queue | `PriorityQueue` | O(log n) insert, O(1) peek |

**The most common mistake:** using `LinkedList` when you want a queue. `ArrayDeque` is faster for both stack and queue operations.

### The Map API — Every Method You'll Actually Use

```java
Map<String, Integer> scores = new HashMap<>();

// The basics
scores.put("Alice", 95);
scores.get("Alice");              // 95
scores.getOrDefault("Bob", 0);    // 0 — safe!
scores.containsKey("Alice");      // true
scores.putIfAbsent("Alice", 100); // does nothing — Alice already exists

// Power methods
scores.compute("Alice", (k, v) -> v == null ? 1 : v + 1);  // increment or init
scores.merge("Bob", 5, Integer::sum);  // add 5 to Bob, or set 5 if absent

// Iterating — the idiomatic way
scores.forEach((name, score) -> System.out.println(name + ": " + score));

// Immutable (Java 9+) — use for config, lookup tables
Map<String, Integer> codes = Map.of("NYC", 212, "LA", 310, "SF", 415);
```

### Streams — Think SQL, Not Loops

A stream is a lazy pipeline of operations. Nothing runs until you call a terminal operation.

```
DATA SOURCE  →  INTERMEDIATE OPS (lazy)  →  TERMINAL OP (triggers everything)
List.stream()    .filter()  .map()  .sorted()     .collect()  .count()  .findFirst()
```

**The mental model:**
```java
// SQL: SELECT name FROM users WHERE age >= 30 ORDER BY name LIMIT 10
List<String> result = users.stream()
    .filter(u -> u.age() >= 30)       // WHERE age >= 30
    .sorted(comparing(User::name))    // ORDER BY name
    .map(User::name)                  // SELECT name
    .limit(10)                        // LIMIT 10
    .toList();                        // execute it
```

### Collectors — Group, Count, Join, Partition

```java
// Group by city — like SQL GROUP BY
Map<String, List<Person>> byCity = people.stream()
    .collect(Collectors.groupingBy(Person::city));

// Count per group
Map<String, Long> countByCity = people.stream()
    .collect(Collectors.groupingBy(Person::city, Collectors.counting()));

// Average salary by city
Map<String, Double> avgSalary = people.stream()
    .collect(Collectors.groupingBy(Person::city,
        Collectors.averagingDouble(Person::salary)));

// Partition into two groups (true/false)
Map<Boolean, List<Person>> partition = people.stream()
    .collect(Collectors.partitioningBy(p -> p.age() >= 30));

// Join to a string
String names = people.stream()
    .map(Person::name)
    .collect(Collectors.joining(", ", "[", "]")); // "[Alice, Bob, Charlie]"
```

### flatMap — The One That Trips Everyone Up

`map` transforms each element. `flatMap` transforms AND flattens nested structures:

```java
// You have a list of lists
List<List<Integer>> nested = List.of(List.of(1,2,3), List.of(4,5), List.of(6));

// map gives you Stream<List<Integer>> — still nested!
nested.stream().map(list -> list) // Stream<List<Integer>>

// flatMap gives you Stream<Integer> — flattened!
nested.stream().flatMap(List::stream).toList() // [1, 2, 3, 4, 5, 6]
```

Real-world use: a user has multiple orders, each order has multiple items. flatMap lets you get all items across all orders in one pipeline.

### Optional — The NullPointerException Killer

```java
// Old, null-riddled code:
User user = findUser(id);
if (user != null) {
    Address addr = user.getAddress();
    if (addr != null) {
        String city = addr.getCity();
        if (city != null) {
            System.out.println(city.toUpperCase());
        }
    }
}

// Modern, Optional-chained code:
findUser(id)
    .map(User::getAddress)
    .map(Address::getCity)
    .map(String::toUpperCase)
    .ifPresent(System.out::println);
```

**Key Optional methods:**
```java
optional.isPresent()            // is there a value?
optional.isEmpty()              // is it empty? (Java 11+)
optional.get()                  // ⚠️ throws if empty! use rarely
optional.orElse(defaultValue)   // value or default
optional.orElseGet(() -> compute()) // lazy default (better for expensive ops)
optional.orElseThrow(()         -> new NotFoundException("x"))
optional.ifPresent(val -> ...)  // do something if present
optional.map(val -> transform)  // transform if present
optional.filter(val -> test)    // filter: empty if predicate is false
```

---

## Chapter 4 — Modern Java (8–21)

> **File:** `04-modern-java/ModernJava.java`

### Lambdas Are Anonymous Functions

A lambda is a compact way to write a one-method anonymous class. Anywhere Java expects a functional interface (an interface with exactly one abstract method), you can pass a lambda.

```java
// Verbose anonymous class:
Comparator<String> comp = new Comparator<String>() {
    @Override
    public int compare(String a, String b) { return a.compareTo(b); }
};

// Lambda — same thing, 1 line:
Comparator<String> comp = (a, b) -> a.compareTo(b);

// Method reference — even shorter when a method does exactly what you need:
Comparator<String> comp = String::compareTo;
```

### The Core Functional Interfaces

These four cover ~90% of what you'll need:

```java
// Predicate<T> — takes T, returns boolean (test something)
Predicate<String> isEmail = s -> s.contains("@");
Predicate<String> isLong  = s -> s.length() > 10;
isEmail.and(isLong).test("test@example.com"); // compose predicates!

// Function<T,R> — takes T, returns R (transform)
Function<String, Integer> length = String::length;
Function<Integer, String> toStr = n -> "Count: " + n;
Function<String, String> composed = length.andThen(toStr); // chain them
composed.apply("hello"); // "Count: 5"

// Consumer<T> — takes T, returns void (side effect)
Consumer<String> log = System.out::println;
Consumer<String> audit = s -> auditLog.write(s);
log.andThen(audit).accept("important event"); // runs both!

// Supplier<T> — takes nothing, returns T (factory)
Supplier<List<String>> listFactory = ArrayList::new;
Supplier<LocalDateTime> now = LocalDateTime::now;
```

### Records — Immutable Data Classes Without the Boilerplate

Before records, a simple data class required: constructor, getters, equals, hashCode, toString — 40+ lines for 3 fields. Records do all of it:

```java
// Before records (Java 7-14):
public final class Point {
    private final double x;
    private final double y;
    // constructor, getters, equals, hashCode, toString... 40 lines

// After records (Java 16+):
record Point(double x, double y) {} // that's it.

Point p = new Point(3.0, 4.0);
p.x();           // accessor (not getX — no "get" prefix!)
p.toString();    // Point[x=3.0, y=4.0]
p.equals(new Point(3.0, 4.0)); // true — value equality

// Add validation in the compact constructor:
record Point(double x, double y) {
    Point { // compact — no parameters
        if (x < 0 || y < 0) throw new IllegalArgumentException("No negative coords");
    }
}
```

Use records for: DTOs, value objects, config, anything that's "just data."

### Sealed Classes — Making Illegal States Impossible

Sealed classes let you declare exactly which classes can extend yours. Combined with pattern matching, the compiler can verify you've handled every case:

```java
sealed interface Payment permits CreditCard, PayPal, BankTransfer {}
record CreditCard(String number, String cvv) implements Payment {}
record PayPal(String email) implements Payment {}
record BankTransfer(String iban) implements Payment {}

// Compiler verifies ALL cases are handled — no default needed!
String process(Payment payment) {
    return switch (payment) {
        case CreditCard cc    -> "Charging card ending in " + cc.number().substring(12);
        case PayPal pp        -> "Charging PayPal account: " + pp.email();
        case BankTransfer bt  -> "Initiating bank transfer to: " + bt.iban();
    };
}
```

If you add a new `Payment` type and forget to update this switch, the **compiler** tells you. This is impossible with regular inheritance.

### The Date/Time API — Finally, a Good One

Java's original `Date` and `Calendar` classes are genuinely terrible. Java 8 replaced them with an immutable, thread-safe API inspired by Joda-Time:

```java
// Which type should I use?
LocalDate      // date only, no time, no timezone (birthday, anniversary)
LocalTime      // time only, no date, no timezone (store hours: "opens at 9am")
LocalDateTime  // date + time, no timezone (form input, log timestamps in local time)
ZonedDateTime  // date + time + timezone (scheduling, user-facing times)
Instant        // machine timestamp — nanoseconds since epoch (storage, comparison)

// All are IMMUTABLE — "modification" returns a new object
LocalDate today    = LocalDate.now();
LocalDate tomorrow = today.plusDays(1);  // new object, today unchanged
LocalDate birthday = LocalDate.of(1990, Month.JUNE, 15);

// Comparisons
today.isBefore(tomorrow);  // true
today.isAfter(birthday);   // true

// Parsing and formatting
DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
LocalDate parsed    = LocalDate.parse("2024-06-15", fmt);
String formatted    = parsed.format(fmt); // "2024-06-15"
```

---

## Chapter 5 — Spring Core: IoC & Dependency Injection

> **File:** `05-spring-core/SpringCore.java`

### The Big Idea: Inversion of Control

Normally, your code is in charge — it creates objects and wires them together. With IoC, the framework is in charge. You declare what you need; Spring creates and delivers it.

Think of it as Amazon vs going to every store yourself. You declare "I need UserRepository, EmailService, and Logger." Spring figures out how to create them, in what order, with what dependencies, and hands them to you ready to use.

### Why Dependency Injection Is Worth It

Without DI, testing is painful:
```java
// Without DI — UserService is coupled to a specific implementation
class UserService {
    private UserRepository repo = new PostgresUserRepository(
        new DataSource(new JdbcConfig("localhost", 5432, "mydb")) // ugh
    );
}
// To test this, you need a real database. Every. Single. Test.
```

With DI:
```java
// With DI — UserService doesn't know or care how the repository was created
@Service
class UserService {
    private final UserRepository repo;
    UserService(UserRepository repo) { this.repo = repo; }
}

// In tests: just pass in a mock!
UserService service = new UserService(mock(UserRepository.class));
```

### Spring Stereotype Annotations

Spring scans your packages for these annotations and manages those classes:

```java
@Component   // generic — use when nothing else fits
@Service     // business logic layer (semantic marker + enables some AOP features)
@Repository  // data access layer (also translates DB exceptions to Spring exceptions)
@Controller  // web layer (handles HTTP requests)
@RestController // web layer + auto-convert return values to JSON
```

They're all technically equivalent for DI purposes, but using the right one communicates intent.

### Constructor Injection — Always the Right Choice

Three ways to inject, but one is clearly best:

```java
// ✅ CONSTRUCTOR INJECTION — best
@Service
class UserService {
    private final UserRepository repo;  // final! immutable after construction
    private final EmailService email;

    // Spring 4.3+: @Autowired optional when there's one constructor
    public UserService(UserRepository repo, EmailService email) {
        this.repo = repo;
        this.email = email;
    }
    // Testable: new UserService(mockRepo, mockEmail) — no Spring needed
}

// ⚠️ FIELD INJECTION — convenient, but bad
@Service
class UserService {
    @Autowired private UserRepository repo;    // Can't make it final
    @Autowired private EmailService email;     // Needs Spring to test
}

// ✅ SETTER INJECTION — for optional dependencies
@Service
class UserService {
    private NotificationService ns;

    @Autowired(required = false)  // won't crash if no NotificationService exists
    public void setNotificationService(NotificationService ns) { this.ns = ns; }
}
```

### Bean Scopes

By default, Spring creates **one instance** of each bean and shares it everywhere. This is the **singleton** scope. For stateless services, this is exactly what you want.

```java
@Component
// @Scope("singleton")  ← this is the implicit default
class UserService {
    // One instance shared across the entire application
}

@Component
@Scope("prototype")
class ShoppingCart {
    private List<Item> items = new ArrayList<>();
    // A NEW instance is created every time someone requests this bean
    // Use for stateful objects that shouldn't be shared
}
```

Web-only scopes:
- `request` — new instance per HTTP request
- `session` — new instance per HTTP session

### @Configuration Classes — When You Need Manual Wiring

When you can't annotate a class (third-party library, complex setup), define beans manually:

```java
@Configuration
class DatabaseConfig {

    @Bean
    public DataSource dataSource(DataSourceProperties props) {
        return DataSourceBuilder.create()
            .url(props.getUrl())
            .username(props.getUsername())
            .password(props.getPassword())
            .build();
    }

    @Bean
    @ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
    public CacheManager cacheManager() {
        return new CaffeineCacheManager();
    }
}
```

### @Value vs @ConfigurationProperties

For 1-2 values: `@Value`. For related config: `@ConfigurationProperties`.

```java
// ❌ Scattered @Value (gets messy with many properties)
@Service
class EmailService {
    @Value("${email.host}") private String host;
    @Value("${email.port}") private int port;
    @Value("${email.from}") private String from;
}

// ✅ Grouped @ConfigurationProperties (cleaner, testable, validated)
@ConfigurationProperties(prefix = "email")
record EmailProperties(String host, int port, String from) {}

@Service
class EmailService {
    private final EmailProperties props;
    EmailService(EmailProperties props) { this.props = props; }
}
```

---

## Chapter 6 — Spring Boot

> **File:** `06-spring-boot/SpringBoot.java`

### What is Spring Boot?

Before Spring Boot existed, building a Spring application was a real workout. You'd wrestle with XML configuration files, manually wire up every dependency, hunt down compatible library versions, and configure an external server just to get a "Hello, World" running. It was powerful — but exhausting.

**Spring Boot changed everything.**

Released in 2014, Spring Boot is built on top of the Spring Framework but takes an entirely different philosophy: **convention over configuration**. Instead of you telling Spring exactly how to set everything up, Spring Boot makes smart, opinionated defaults and sets things up for you automatically. You focus on writing business logic; Spring Boot handles the boilerplate.

Here's the 30-second version of what Spring Boot gives you:

| Feature | What it means for you |
|---|---|
| **Auto-configuration** | Spring Boot detects what's on your classpath and configures it automatically. Add `spring-boot-starter-web`? You instantly have a web server, JSON serialization, and routing — no setup required. |
| **Starter dependencies** | Curated Maven/Gradle dependency bundles (e.g., `spring-boot-starter-data-jpa`) that pull in everything you need, all at compatible versions. No more dependency hell. |
| **Embedded server** | Tomcat (or Jetty/Undertow) is bundled directly into your app. Your service is a single runnable JAR — `java -jar myapp.jar` and you're live. No separate server installation needed. |
| **`application.yml` / `application.properties`** | One central place to configure your entire application — ports, database connections, log levels, feature flags, and more. |
| **Spring Boot Actuator** | Production-ready endpoints (`/actuator/health`, `/actuator/metrics`) baked in with a single dependency. |
| **Spring Initializr** | [start.spring.io](https://start.spring.io) generates a fully working project skeleton in seconds. |

The result: a new Spring Boot project goes from zero to a running web service in minutes, not hours. That's why it's become the dominant way to build Java backend services across the industry.

### How a Spring Boot App is Structured

A typical Spring Boot project follows a layered, package-by-feature structure:

```
com.example.myapp/
├── MyAppApplication.java       ← Entry point — contains main()
│
├── controller/                 ← HTTP layer: receives requests, returns responses
│   └── UserController.java
│
├── service/                    ← Business logic lives here
│   └── UserService.java
│
├── repository/                 ← Data access layer (Spring Data JPA interfaces)
│   └── UserRepository.java
│
├── model/ (or domain/)         ← Your entities, records, and domain objects
│   └── User.java
│
└── config/                     ← Configuration classes (@Bean definitions, security, etc.)
    └── SecurityConfig.java
```

This layered architecture keeps concerns separated and your codebase navigable. Controllers talk to services; services talk to repositories; repositories talk to the database. Each layer has one job, and that discipline pays off massively as your application grows.

Now let's see how it all comes together, starting with the entry point.

---

### What @SpringBootApplication Actually Does

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

This single annotation is shorthand for three things:
- `@Configuration` — this class can define beans
- `@EnableAutoConfiguration` — let Spring Boot auto-configure everything it can
- `@ComponentScan` — scan this package (and sub-packages) for `@Component` etc.

Auto-configuration means: Spring Boot sees `spring-boot-starter-web` on the classpath → configures an embedded Tomcat on port 8080, a `DispatcherServlet`, Jackson for JSON, and much more — without any XML or code from you.

### application.yml — Your App's Control Panel

```yaml
spring:
  application:
    name: my-service

  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: ${DB_USERNAME}          # from environment variable
    password: ${DB_PASSWORD}          # NEVER hardcode credentials
    hikari:
      maximum-pool-size: 10           # connection pool size

  jpa:
    hibernate:
      ddl-auto: validate              # validate | update | create | create-drop
    show-sql: false                   # true during development only

server:
  port: 8080

logging:
  level:
    root: INFO
    com.myapp: DEBUG                  # your package gets more detail

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics  # expose these actuator endpoints
```

### Profiles — Different Configs for Different Environments

```yaml
# application.yml (base config — all environments)
app:
  name: My App

---
# application-development.yml
spring:
  datasource:
    url: jdbc:h2:mem:devdb    # in-memory DB in dev
  jpa:
    show-sql: true            # verbose SQL in dev only

---
# application-production.yml
spring:
  datasource:
    url: jdbc:postgresql://prod-cluster:5432/appdb
logging:
  level:
    root: WARN                # quieter in production
```

Activate profiles:
```bash
# Environment variable (most common in containers/CI):
SPRING_PROFILES_ACTIVE=production java -jar app.jar

# Command line:
java -jar app.jar --spring.profiles.active=production

# In tests:
@ActiveProfiles("test")
class MyTest { ... }
```

### Spring Boot Actuator — Production Observability for Free

Add `spring-boot-starter-actuator` and you instantly get:

| Endpoint | What it gives you |
|---|---|
| `GET /actuator/health` | Is the app up? Is the DB connected? |
| `GET /actuator/metrics` | JVM memory, CPU, HTTP request counts, etc. |
| `GET /actuator/info` | App version, build info |
| `GET /actuator/env` | All resolved properties |
| `GET /actuator/beans` | Every bean in the Spring context |
| `GET /actuator/mappings` | Every `@RequestMapping` endpoint |
| `GET /actuator/loggers` | View/change log levels at runtime! |

Custom health indicator:
```java
@Component
class DatabaseHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        if (isDatabaseReachable()) {
            return Health.up().withDetail("response_time", "3ms").build();
        }
        return Health.down().withDetail("error", "Cannot connect").build();
    }
}
```

---

## Chapter 7 — Spring Data JPA

> **File:** `07-spring-data/SpringData.java`

### The Stack Explained

```
Your Code
    ↓
Spring Data JPA (JpaRepository, @Query, derived queries)
    ↓
JPA (Java Persistence API — specification)
    ↓
Hibernate (JPA implementation — actually writes the SQL)
    ↓
JDBC
    ↓
Your Database (PostgreSQL, MySQL, H2, etc.)
```

You write Java. Hibernate writes SQL. You (usually) win.

### Entity Mapping — The Key Annotations

```java
@Entity                         // "this class maps to a DB table"
@Table(name = "users")          // table name (default = class name)
class User {

    @Id                          // primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB auto-increment
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING) // store "ACTIVE", not 0/1
    private UserStatus status;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders;  // "mappedBy" = the FK is on the Order side

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;
}
```

**FetchType explained:**
- `LAZY` (default for collections) — load when accessed. Usually what you want.
- `EAGER` — load immediately with the parent. Can cause performance problems with collections.

### JpaRepository — CRUD for Free

```java
// Extend JpaRepository<EntityType, PrimaryKeyType>
interface UserRepository extends JpaRepository<User, Long> {}

// You now have, for free:
userRepository.save(user);           // INSERT or UPDATE
userRepository.findById(1L);         // SELECT by PK
userRepository.findAll();            // SELECT *
userRepository.findAll(pageable);    // SELECT * with pagination
userRepository.delete(user);         // DELETE
userRepository.count();              // SELECT COUNT(*)
userRepository.existsById(1L);       // SELECT EXISTS(...)
```

### Derived Query Methods — Spring Reads Your Method Names

```java
interface UserRepository extends JpaRepository<User, Long> {
    // Spring generates SQL from the method name:
    Optional<User> findByEmail(String email);
    // → SELECT * FROM users WHERE email = ?

    List<User> findByStatusAndAgeGreaterThan(UserStatus status, int minAge);
    // → SELECT * FROM users WHERE status = ? AND age > ?

    List<User> findByNameContainingIgnoreCase(String fragment);
    // → SELECT * FROM users WHERE UPPER(name) LIKE UPPER('%?%')

    boolean existsByEmail(String email);
    // → SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)

    long countByStatus(UserStatus status);
    // → SELECT COUNT(*) FROM users WHERE status = ?

    List<User> findTop5ByStatusOrderByCreatedAtDesc(UserStatus status);
    // → SELECT * FROM users WHERE status = ? ORDER BY created_at DESC LIMIT 5
}
```

If you can read the method name, you know the SQL.

### @Query — When Method Names Get Unwieldy

```java
// JPQL (entity names + field names, not table/column names)
@Query("SELECT u FROM User u WHERE u.email LIKE %:domain%")
List<User> findByEmailDomain(@Param("domain") String domain);

// Native SQL when you need database-specific features
@Query(value = "SELECT * FROM users WHERE created_at > NOW() - INTERVAL '7 days'",
       nativeQuery = true)
List<User> findRecentUsers();

// Modifying queries — MUST have @Modifying AND @Transactional
@Modifying
@Transactional
@Query("UPDATE User u SET u.status = 'INACTIVE' WHERE u.lastLogin < :cutoff")
int deactivateInactiveUsers(@Param("cutoff") LocalDateTime cutoff);
```

### The N+1 Problem — Every JPA Developer's First Scar

This is the most common JPA performance mistake. If you load a list of 100 orders and then access `order.getUser()` on each, JPA fires a separate query for EACH user — 101 queries instead of 1.

```java
// 💣 N+1 problem:
List<Order> orders = orderRepository.findAll(); // 1 query for orders
orders.forEach(o -> System.out.println(o.getUser().getName())); // 100 queries for users!

// ✅ Fix 1: JOIN FETCH in JPQL
@Query("SELECT o FROM Order o JOIN FETCH o.user WHERE o.status = :status")
List<Order> findByStatus(OrderStatus status);

// ✅ Fix 2: @EntityGraph (declarative, no JPQL needed)
@EntityGraph(attributePaths = {"user", "items"})
List<Order> findByStatus(OrderStatus status);
```

### Pagination — Because Nobody Needs All 500,000 Users

```java
// In your controller:
@GetMapping("/users")
Page<UserResponse> getUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

    Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
    Page<User> userPage = userRepository.findAll(pageable);

    return userPage.map(user -> toResponse(user)); // Page has .map()!
}

// Page<T> gives you:
page.getContent()       // List<T> — current page items
page.getTotalElements() // total count across all pages
page.getTotalPages()    // number of pages
page.getNumber()        // current page number (0-indexed)
page.hasNext()          // is there a next page?
```

---

## Chapter 8 — Spring Security

> **File:** `08-spring-security/SpringSecurity.java`

### The Core Concept: The Filter Chain

Every HTTP request passes through a chain of filters before reaching your controller. Spring Security adds its own filters. The most important: `JwtAuthenticationFilter` (for stateless APIs) or `UsernamePasswordAuthenticationFilter` (for form login).

### The Modern Security Config

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())   // stateless API — CSRF not needed
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS)) // no server sessions
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()   // login/register = public
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(DELETE, "/api/**").hasRole("ADMIN")
                .anyRequest().authenticated()                 // everything else = must login
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // strength 12 — slow by design
    }
}
```

### JWT Authentication Flow

```
Client                          Server
  │                               │
  │── POST /api/auth/login ──────→│
  │   { email, password }         │
  │                               │── 1. Verify credentials
  │                               │── 2. Generate JWT
  │←── 200 OK ────────────────────│
  │    { accessToken: "eyJ..." }  │
  │                               │
  │── GET /api/users ────────────→│
  │   Authorization: Bearer eyJ...│── 1. JwtAuthenticationFilter
  │                               │── 2. Verify JWT signature
  │                               │── 3. Set SecurityContext
  │                               │── 4. Pass to controller
  │←── 200 OK ────────────────────│
```

### Method-Level Security — The Fine-Grained Control

```java
@EnableMethodSecurity // enable this in your config

@Service
class UserService {

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) { ... }

    // Combined conditions
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public User getUser(Long id) { ... }

    // Check permission
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public User createUser(CreateUserRequest req) { ... }
}
```

### Never Store Plain Passwords — Ever

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}

// When creating a user:
String hashed = passwordEncoder.encode(rawPassword);  // one-way hash
user.setPasswordHash(hashed);

// When verifying login:
passwordEncoder.matches(rawInput, storedHash);  // true/false
// BCrypt is deliberately slow — protects against brute force
```

---

## Chapter 9 — Building REST APIs

> **File:** `09-spring-rest/SpringRest.java`

### The Anatomy of a Spring REST Controller

```java
@RestController                     // handles HTTP, auto-serialises to JSON
@RequestMapping("/api/v1/products") // base path
class ProductController {

    @GetMapping("/{id}")            // GET /api/v1/products/42
    ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) { ... }

    @PostMapping                    // POST /api/v1/products
    ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest req) { ... }

    @PutMapping("/{id}")            // PUT /api/v1/products/42
    ResponseEntity<ProductResponse> replaceProduct(
            @PathVariable Long id,
            @Valid @RequestBody CreateProductRequest req) { ... }

    @PatchMapping("/{id}")          // PATCH /api/v1/products/42
    ResponseEntity<ProductResponse> patchProduct(
            @PathVariable Long id,
            @RequestBody UpdateProductRequest req) { ... }

    @DeleteMapping("/{id}")         // DELETE /api/v1/products/42
    ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build(); // 204
    }
}
```

### DTOs — Never Expose Your Entities Directly

This is non-negotiable in production code. Exposing `@Entity` objects from your controllers:
- Leaks database structure to clients
- Creates bidirectional Jackson serialisation issues
- Couples your API contract to your DB schema

```java
// Entity — your database representation (stays server-side)
@Entity class User { ... }

// Request DTO — what the client sends
record CreateUserRequest(
    @NotBlank String name,
    @Email String email,
    @Size(min=8) String password
) {}

// Response DTO — what you send back (you control exactly what's visible)
record UserResponse(Long id, String name, String email, LocalDateTime createdAt) {}
// Note: no password hash, no internal IDs, no audit fields you don't want exposed
```

### Bean Validation — Validate at the Door

```java
record CreateUserRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    String name,

    @NotBlank @Email String email,

    @NotNull
    @Min(0) @Max(1000)
    Integer age,

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
    String phone
) {}

// In your controller — @Valid triggers validation
@PostMapping
ResponseEntity<?> create(@Valid @RequestBody CreateUserRequest req) { ... }
// If validation fails, Spring returns 400 Bad Request automatically
// (if you have a @RestControllerAdvice that handles MethodArgumentNotValidException)
```

### Global Exception Handling — The Pattern

```java
@RestControllerAdvice
class GlobalExceptionHandler {

    // Map each exception type to the right HTTP status
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404).body(errorResponse(404, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e ->
            fieldErrors.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.status(400).body(/* ... */);
    }

    // Catch-all — NEVER return stack traces to clients
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        log.error("Unexpected error", ex); // log it server-side
        return ResponseEntity.status(500)
            .body(errorResponse(500, "An unexpected error occurred")); // sanitised message
    }
}
```

### REST API Design Best Practices

| Pattern | Example | Why |
|---|---|---|
| Plural nouns for resources | `/users`, `/orders` | Consistent convention |
| IDs in the path | `/users/{id}` | REST semantics |
| Filters as query params | `/users?status=ACTIVE&city=NYC` | Flexible, cacheable |
| Versioning in path | `/api/v1/users` | Allows breaking changes |
| Use correct HTTP methods | GET/POST/PUT/PATCH/DELETE | Idempotency matters |
| Return 201 + Location on create | `Location: /api/v1/users/42` | RFC 7231 compliant |
| Return 204 on delete | No body | Nothing to return |
| Consistent error response | `{ status, error, message, timestamp }` | Client-friendly |

---

## Chapter 10 — Testing

> **File:** `10-testing/Testing.java`

### The Testing Pyramid

```
        /\        ← E2E tests (few, slow, test the full stack)
       /  \
      /    \      ← Integration tests (some, medium speed, test slices)
     /      \
    /        \    ← Unit tests (many, fast, test one class)
   /──────────\
```

Write **lots of fast unit tests**. Write **some integration tests** for critical paths. Write **few E2E tests** for the most important user journeys.

### JUnit 5 + Mockito — The Basics

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepo;     // Mockito creates a fake
    @Mock EmailService emailService;   // another fake
    @InjectMocks UserService service;  // real service, fakes injected

    @Test
    @DisplayName("Should create user and send welcome email")
    void createUser_success() {
        // GIVEN
        when(userRepo.existsByEmail("alice@test.com")).thenReturn(false);
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        User result = service.createUser("Alice", "alice@test.com");

        // THEN
        assertThat(result.email()).isEqualTo("alice@test.com");
        verify(emailService).sendWelcomeEmail("alice@test.com", "Alice");
        verify(emailService, never()).sendPasswordResetEmail(any());
    }

    @Test
    void createUser_duplicateEmail_throws() {
        when(userRepo.existsByEmail(any())).thenReturn(true);

        assertThatThrownBy(() -> service.createUser("Alice", "alice@test.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already registered");
    }
}
```

### @WebMvcTest — Test Your Controllers Without a Full Context

```java
@WebMvcTest(UserController.class)  // only loads web layer
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean UserService userService;  // mock the service layer

    @Test
    void getUser_shouldReturn200() throws Exception {
        when(userService.getUser(1L)).thenReturn(new UserResponse(1L, "Alice", "alice@test.com"));

        mockMvc.perform(get("/api/v1/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void createUser_invalidRequest_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "name": "", "email": "not-an-email" }"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.name").exists());
    }
}
```

### @DataJpaTest — Test Your Repositories Against a Real (H2) DB

```java
@DataJpaTest  // loads JPA, H2, repositories — nothing else
class UserRepositoryTest {

    @Autowired UserRepository repo;
    @Autowired TestEntityManager em;

    @Test
    void findByEmail_shouldReturnUser() {
        em.persistAndFlush(new User("Alice", "alice@test.com"));

        Optional<User> found = repo.findByEmail("alice@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Alice");
    }
}
```

### Testcontainers — Real PostgreSQL in Tests, Zero Config

```java
@SpringBootTest
@Testcontainers
class RepositoryIntegrationTest {

    @Container  // starts a real PostgreSQL Docker container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void shouldWorkWithRealDatabase() {
        // This test runs against a real PostgreSQL, not H2!
        // Same behaviour as production.
    }
}
```

### Test Best Practices

1. **One assertion concept per test** — if a test fails, you know exactly what broke
2. **Descriptive test names** — `when_userNotFound_should_throwNotFoundException`
3. **Arrange-Act-Assert** — clear structure in every test
4. **Don't test the framework** — don't test that Spring wires beans; test your logic
5. **Use @BeforeEach for shared setup** — extract repeated setup
6. **Test edge cases** — null inputs, empty lists, boundary values
7. **Keep tests fast** — unit tests should run in milliseconds

---

## Chapter 11 — Real-World Patterns

> **File:** `11-real-world-patterns/Patterns.java`

### AOP — Stop Repeating Yourself Across Every Method

Aspect-Oriented Programming lets you define behaviour that runs around methods matching a pattern — without touching those methods.

```java
// Without AOP — you'd repeat this logging in every service method:
void createUser(String name, String email) {
    log.info("→ createUser({}, {})", name, email);
    long start = System.currentTimeMillis();
    try {
        // actual logic
        log.info("← createUser returned in {}ms", System.currentTimeMillis() - start);
    } catch (Exception e) {
        log.error("✗ createUser failed: {}", e.getMessage());
        throw e;
    }
}

// With AOP — define ONCE, applies to ALL service methods:
@Aspect @Component
class LoggingAspect {
    @Around("execution(* com.example.service.*.*(..))")
    Object logAll(ProceedingJoinPoint jp) throws Throwable {
        // log entry, time, log exit/error — for every service method, automatically
    }
}
```

Spring uses AOP internally for `@Transactional`, `@Cacheable`, `@Async`, and `@Scheduled`. Understanding AOP helps you understand why those annotations have certain limitations (e.g., internal method calls don't get intercepted).

### Application Events — Decouple Your Side Effects

```java
// ❌ UserService with too many responsibilities:
class UserService {
    void registerUser(String name, String email) {
        userRepo.save(new User(name, email));
        emailService.sendWelcome(email);          // now depends on email
        analyticsService.trackRegistration(email); // now depends on analytics
        notificationService.notify(email);         // now depends on notifications
        // Add a new side effect → modify UserService. Wrong.
    }
}

// ✅ UserService does ONE thing, publishes an event:
class UserService {
    void registerUser(String name, String email) {
        User user = userRepo.save(new User(name, email));
        events.publishEvent(new UserRegisteredEvent(user.id(), email, name));
        // Done. UserService has no idea what happens next.
    }
}

// Each listener handles its own concern, independently:
@Component class WelcomeEmailListener {
    @EventListener void on(UserRegisteredEvent e) { emailService.sendWelcome(e.email()); }
}
@Component class AnalyticsListener {
    @Async @EventListener void on(UserRegisteredEvent e) { analytics.track(e); } // async!
}
```

### Caching — Don't Compute the Same Thing Twice

```java
@Service
class ProductService {
    @Cacheable(value = "products", key = "#id")
    public Product findById(Long id) {
        // This only runs on cache MISS. Otherwise returns cached value.
        System.out.println("Loading from DB..."); // you'll only see this once per ID
        return productRepo.findById(id).orElseThrow();
    }

    @CachePut(value = "products", key = "#result.id")
    public Product update(Product product) {
        return productRepo.save(product); // updates DB AND cache
    }

    @CacheEvict(value = "products", key = "#id")
    public void delete(Long id) {
        productRepo.deleteById(id); // removes from DB AND cache
    }
}
```

Caffeine cache config in `application.yml`:
```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=10m  # max 1000 entries, expire after 10 min
```

### The Circuit Breaker — Fail Fast, Recover Gracefully

When an external service is down, don't let it bring down your service too. A circuit breaker tracks failures and "opens" (stops calling the broken service) after a threshold:

```
CLOSED (normal)  →  failure rate > 50%  →  OPEN (fast-fail, use fallback)
    ↑                                           ↓
    ←───── after 30s, try again ─────── HALF-OPEN (test one request)
```

```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
@Retry(name = "paymentService")
public String processPayment(double amount) {
    return externalPaymentApi.charge(amount); // might fail
}

private String paymentFallback(double amount, Exception e) {
    log.warn("Payment service unavailable, queuing for retry");
    paymentQueue.add(amount); // queue for later
    return "QUEUED";
}
```

---

## Chapter 12 — Production Readiness

This chapter covers what separates a "working" app from a "production-grade" app.

### Database Migrations with Flyway

Never let Hibernate manage your schema in production (`ddl-auto: validate` only!). Use Flyway for version-controlled schema migrations:

```
src/main/resources/db/migration/
    V1__create_users_table.sql
    V2__add_user_status_column.sql
    V3__create_orders_table.sql
    V4__add_user_email_index.sql
```

```sql
-- V1__create_users_table.sql
CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

Flyway tracks which migrations have run in a `flyway_schema_history` table. Never edit an existing migration — always add new ones.

### Logging Best Practices

```java
@Slf4j  // Lombok annotation — creates `private static final Logger log`
@Service
class UserService {

    public User createUser(String name, String email) {
        log.debug("Creating user: name={}, email={}", name, email); // dev detail
        User saved = userRepository.save(new User(name, email));
        log.info("User created: id={}, email={}", saved.getId(), email); // business event
        return saved;
    }

    public void riskyOperation() {
        try {
            externalApi.call();
        } catch (ExternalApiException e) {
            log.error("External API failed: service={}, error={}", "payment-api", e.getMessage(), e);
            // Include the exception object — it captures the stack trace in logs
            throw new ServiceUnavailableException("Payment service unavailable");
        }
    }
}
```

**Logging levels:**
- `ERROR` — something broke, needs immediate attention
- `WARN` — something unexpected happened, app is still working
- `INFO` — business events, app lifecycle (startup, shutdown, user actions)
- `DEBUG` — detailed diagnostic info for development
- `TRACE` — extremely verbose (every SQL query, every method call)

### Graceful Shutdown

```yaml
server:
  shutdown: graceful   # wait for in-flight requests to complete

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s  # max 30s to finish in-flight requests
```

### Docker-Ready Configuration

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
```

---

## Quick Reference & Interview Prep

### The 25 Questions Every Java/Spring Interviewer Asks

---

**Q1: What's the difference between `==` and `.equals()`?**
`==` compares references (memory addresses). `.equals()` compares content. Always use `.equals()` for objects, especially Strings.

---

**Q2: What is the Java Memory Model — heap vs stack?**
- **Stack**: method call frames, local variables, primitive values. Fast. LIFO. Per-thread.
- **Heap**: all objects (created with `new`). Slower. Shared across threads. Managed by GC.

---

**Q3: What is a checked vs unchecked exception?**
- **Checked**: extends `Exception` (not `RuntimeException`). Must be declared with `throws` or caught. Use for recoverable errors (file not found, network timeout).
- **Unchecked**: extends `RuntimeException`. No obligation to catch. Use for programming errors (null pointer, array bounds).
- **Spring best practice**: use unchecked exceptions everywhere. Checked exceptions are an anti-pattern in modern Java.

---

**Q4: Explain IoC and DI.**
IoC (Inversion of Control): instead of your code creating dependencies, a container (Spring) creates and injects them. DI is the most common IoC mechanism — you declare what you need (constructor parameters), Spring provides it. Benefits: testability, decoupling, lifecycle management.

---

**Q5: What is a Spring Bean?**
Any object managed by the Spring ApplicationContext. Beans are created, configured, and destroyed by Spring. Declare them with `@Component`/`@Service`/etc. or `@Bean` in a `@Configuration` class.

---

**Q6: What's the difference between `@Component`, `@Service`, and `@Repository`?**
Functionally identical for DI — all register a bean. The difference is semantic/functional:
- `@Repository` additionally translates DB-specific exceptions to Spring's `DataAccessException` hierarchy.
- `@Service` is a marker for the business logic layer.
- Using the right annotation communicates intent to other developers.

---

**Q7: What is bean scope? What are the common scopes?**
Scope determines how many instances Spring creates. Common scopes:
- `singleton` (default): one shared instance per ApplicationContext
- `prototype`: new instance every time it's requested
- `request`/`session`: one instance per HTTP request/session (web apps only)

---

**Q8: What is constructor injection and why is it preferred?**
Inject dependencies through the constructor, not fields. Benefits: dependencies can be `final` (immutable); the class is testable without Spring (just `new MyService(mockDep)`); missing dependencies cause compile errors, not runtime NPEs.

---

**Q9: What does `@Transactional` do?**
Wraps a method in a database transaction. If the method completes successfully, the transaction commits. If a `RuntimeException` is thrown, it rolls back. Key settings:
- `readOnly = true`: optimisation hint for read-only methods
- `rollbackFor`: which exceptions trigger rollback
- `propagation`: how nested transactions behave (REQUIRED, REQUIRES_NEW, etc.)

---

**Q10: What is the N+1 problem?**
Loading N entities with LAZY relationships, then accessing a relationship on each one, fires N additional queries (total N+1). Fix with JOIN FETCH, `@EntityGraph`, or `@BatchSize`.

---

**Q11: What's the difference between `@OneToMany` and `@ManyToOne`?**
`@OneToMany` is on the "one" side (User has many Orders — goes on User). `@ManyToOne` is on the "many" side (Order belongs to one User — goes on Order, has the FK column). The "many" side owns the relationship (`mappedBy` goes on the "one" side).

---

**Q12: How does Spring Security authenticate a request?**
1. Request enters the `SecurityFilterChain`
2. Your filter (e.g., `JwtAuthenticationFilter`) extracts the token
3. Validate the token signature and expiry
4. Load `UserDetails` from your `UserDetailsService`
5. Create `UsernamePasswordAuthenticationToken` and set it in `SecurityContextHolder`
6. Spring Security checks if the authentication has the required authority for the endpoint

---

**Q13: What is JWT and how does it work?**
JSON Web Token = `base64(header).base64(claims).signature`. The server signs it with a secret key. Clients send it in `Authorization: Bearer <token>`. The server verifies the signature (no DB lookup needed — it's stateless). Contains: who the user is, their roles, expiry time.

---

**Q14: What are Streams and what makes them powerful?**
Streams are lazy, declarative pipelines for processing collections. Powerful because: lazy evaluation (no wasted work), composable operations, parallel processing with `parallelStream()`, and expressive code that reads like what you want (filter/map/collect) not how to do it (nested loops).

---

**Q15: What's the difference between `map` and `flatMap`?**
`map` transforms each element 1:1. `flatMap` transforms each element and flattens nested collections. Use `flatMap` when your transform returns a collection and you want a flat result.

---

**Q16: What is Optional and when should you use it?**
A container that may or may not contain a value. Use it as a return type when a method might legitimately return "nothing" (e.g., `findById` might not find the entity). Never use `Optional.get()` without checking — use `orElse`, `orElseThrow`, `ifPresent`, `map`.

---

**Q17: What is AOP and what Spring features use it?**
Aspect-Oriented Programming lets you inject behaviour into methods matching a pattern without modifying those methods. Spring uses AOP for: `@Transactional` (transaction management), `@Cacheable` (caching), `@Async` (async execution), `@Scheduled` (scheduling), security annotations.

---

**Q18: What's the difference between `@SpringBootTest` and `@WebMvcTest`?**
`@SpringBootTest` loads the **full** ApplicationContext — all beans, full Spring, embedded server option. Comprehensive but slow. Use for integration tests.
`@WebMvcTest` loads **only the web layer** (controllers, filters, serialisers). Much faster. Use for unit-testing controller behaviour. Service layer mocked with `@MockBean`.

---

**Q19: How does `@Cacheable` work?**
Spring wraps the method in a proxy. On invocation, it checks the cache for the key. On hit: return cached value (method body skipped). On miss: execute method, store result in cache, return result. Only works when called from OUTSIDE the class (proxy limitation).

---

**Q20: What is the Spring Application Context?**
The IoC container — it creates, manages, and wires all beans. The `ApplicationContext` is the "god object" of a Spring app. You rarely interact with it directly.

---

**Q21: What are `@PreAuthorize` and `@PostAuthorize`?**
Method-level security annotations. `@PreAuthorize` checks before the method runs (most common). `@PostAuthorize` checks after, with access to the return value. Requires `@EnableMethodSecurity`.

---

**Q22: What is Hibernate and what is JPA?**
JPA (Jakarta Persistence API) is a **specification** — it defines how Java objects map to relational DB tables. Hibernate is the most popular **implementation** of JPA — it actually writes the SQL. Spring Data JPA sits on top of JPA and adds repositories, derived queries, and more.

---

**Q23: What is `@EntityGraph`?**
A declarative way to specify which lazy relationships to load eagerly for a specific query. Solves the N+1 problem without writing JPQL. Cleaner than adding `fetch = EAGER` to the entity (which would affect all queries).

---

**Q24: What is a record and when should I use one?**
A record (Java 16+) is an immutable data class. The compiler generates constructor, accessors (`name()` not `getName()`), `equals()`, `hashCode()`, `toString()`. Use for DTOs, value objects, method return types with multiple values. Don't use when you need mutable state or inheritance.

---

**Q25: What's the difference between sealed classes and regular classes?**
Sealed classes (Java 17+) explicitly declare which classes can extend them. This enables exhaustive pattern matching in `switch` — the compiler can verify all cases are handled. Use for domain modelling where you have a known, fixed set of subtypes (payment types, event types, HTTP result types).

---

### Cheat Sheet: Common Annotations

| Annotation | Where | What it does |
|---|---|---|
| `@SpringBootApplication` | Main class | Scans, auto-configures, configures |
| `@Component` | Any class | Register as Spring bean |
| `@Service` | Business logic | Bean + semantic marker |
| `@Repository` | Data access | Bean + DB exception translation |
| `@RestController` | Web layer | Bean + handles HTTP + JSON serialization |
| `@Autowired` | Field/constructor/setter | Inject dependency |
| `@Value("${prop}")` | Field | Inject config value |
| `@ConfigurationProperties` | Class | Bind config prefix to POJO |
| `@Transactional` | Method/class | Wrap in DB transaction |
| `@Entity` | Class | Maps to DB table |
| `@Id` | Field | Primary key |
| `@Query` | Repository method | Custom JPQL or SQL |
| `@PreAuthorize` | Service method | Method-level security |
| `@Cacheable` | Service method | Cache return value |
| `@Async` | Method | Execute in thread pool |
| `@Scheduled` | Method | Run on schedule/cron |
| `@EventListener` | Method | Handle Spring event |
| `@Profile` | Class/bean | Only active for named profile |
| `@Valid` | Method param | Trigger Bean Validation |
| `@RestControllerAdvice` | Class | Global exception handler |

---

*Good luck. You've got this. 🚀*
