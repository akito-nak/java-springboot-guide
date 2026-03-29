package modern;

import java.util.*;
import java.util.function.*;
import java.time.*;

/**
 * ============================================================
 * 04 - MODERN JAVA (Java 8–21)
 * ============================================================
 * Java has evolved dramatically. If you learned Java 7 and stopped,
 * you're missing some genuinely excellent features. This section
 * covers everything that transformed Java from verbose to expressive.
 *
 * Java 8:  Lambdas, Streams, Optional, default methods, new Date/Time API
 * Java 9:  Modules, factory methods (List.of, Map.of)
 * Java 10: var (local type inference)
 * Java 14: Records (preview), switch expressions
 * Java 16: Records (stable), instanceof pattern matching
 * Java 17: Sealed classes (LTS release)
 * Java 21: Virtual threads, sequenced collections (LTS release)
 */
public class ModernJava {

    // ─────────────────────────────────────────────────────────
    // FUNCTIONAL INTERFACES & LAMBDAS
    // ─────────────────────────────────────────────────────────
    // A functional interface has exactly ONE abstract method.
    // A lambda is shorthand for an anonymous class implementing it.

    // Core functional interfaces from java.util.function:
    //   Function<T,R>      — takes T, returns R           (transform)
    //   Consumer<T>        — takes T, returns void        (side effect)
    //   Supplier<T>        — takes nothing, returns T     (factory)
    //   Predicate<T>       — takes T, returns boolean     (test)
    //   BiFunction<T,U,R>  — takes T and U, returns R
    //   UnaryOperator<T>   — Function<T,T>
    //   BinaryOperator<T>  — BiFunction<T,T,T>

    static void lambdaExamples() {
        // Predicate — test something
        Predicate<String> isLong = s -> s.length() > 5;
        Predicate<String> startsWithA = s -> s.startsWith("A");
        Predicate<String> longAndStartsWithA = isLong.and(startsWithA);
        Predicate<String> longOrStartsWithA  = isLong.or(startsWithA);
        Predicate<String> notLong            = isLong.negate();

        System.out.println(isLong.test("Hello World")); // true
        System.out.println(longAndStartsWithA.test("Alexander")); // true

        // Function — transform
        Function<String, Integer> strLen = String::length;
        Function<Integer, String> intToStr = i -> "Number: " + i;
        Function<String, String> composed = strLen.andThen(intToStr); // chain
        System.out.println(composed.apply("Hello")); // "Number: 5"

        // Consumer — side effect
        Consumer<String> print = System.out::println;
        Consumer<String> printUpper = s -> System.out.println(s.toUpperCase());
        Consumer<String> both = print.andThen(printUpper); // chain consumers
        both.accept("hello"); // prints "hello" then "HELLO"

        // Supplier — produce a value
        Supplier<List<String>> listFactory = ArrayList::new;
        List<String> newList = listFactory.get(); // fresh ArrayList each time

        // BiFunction — two inputs
        BiFunction<String, Integer, String> repeat = (s, n) -> s.repeat(n);
        System.out.println(repeat.apply("ha", 3)); // "hahaha"
    }

    // ─────────────────────────────────────────────────────────
    // METHOD REFERENCES — cleaner lambdas
    // ─────────────────────────────────────────────────────────
    //   ClassName::staticMethod    — static method reference
    //   instance::instanceMethod   — bound instance method reference
    //   ClassName::instanceMethod  — unbound instance method reference
    //   ClassName::new             — constructor reference

    static void methodReferences() {
        List<String> names = List.of("Charlie", "Alice", "Bob");

        // Static method reference
        names.stream()
            .map(Integer::parseInt); // same as: s -> Integer.parseInt(s)

        // Unbound instance method — the stream element becomes `this`
        names.stream()
            .map(String::toUpperCase)  // same as: s -> s.toUpperCase()
            .forEach(System.out::println);

        // Bound instance method — captures a specific instance
        String prefix = "Hello, ";
        names.stream()
            .map(prefix::concat)  // same as: s -> prefix.concat(s)
            .forEach(System.out::println);

        // Constructor reference
        List<String> strs = List.of("1", "2", "3");
        strs.stream()
            .map(Integer::new) // same as: s -> new Integer(s)
            .forEach(System.out::println);
    }

    // ─────────────────────────────────────────────────────────
    // RECORDS — immutable data carriers (Java 16+)
    // ─────────────────────────────────────────────────────────
    // Records automatically generate: constructor, getters,
    // equals(), hashCode(), toString(). Perfect for DTOs and value objects.

    record Point(double x, double y) {
        // Compact canonical constructor — for validation
        Point {
            if (Double.isNaN(x) || Double.isNaN(y)) {
                throw new IllegalArgumentException("Coordinates cannot be NaN");
            }
        }

        // Additional methods are allowed
        double distanceTo(Point other) {
            return Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2));
        }

        // Static factory method
        static Point origin() { return new Point(0, 0); }
    }

    record Person(String name, int age, String email) {
        // Records can implement interfaces
    }

    static void recordDemo() {
        Point p1 = new Point(3, 4);
        Point p2 = Point.origin();

        System.out.println(p1);            // Point[x=3.0, y=4.0]
        System.out.println(p1.x());        // 3.0 — accessor (not getX!)
        System.out.println(p1.distanceTo(p2)); // 5.0
        System.out.println(p1.equals(new Point(3, 4))); // true — value equality!
    }

    // ─────────────────────────────────────────────────────────
    // SEALED CLASSES — controlled inheritance (Java 17+)
    // ─────────────────────────────────────────────────────────
    // Declare exactly which classes can extend yours.
    // Perfect for domain modelling — makes illegal states impossible.

    sealed interface Shape permits Circle, Rectangle, Triangle {}

    record Circle(double radius) implements Shape {}
    record Rectangle(double width, double height) implements Shape {}
    record Triangle(double base, double height) implements Shape {}

    // With sealed types + pattern matching, the compiler knows ALL cases
    static double area(Shape shape) {
        return switch (shape) {
            case Circle    c -> Math.PI * c.radius() * c.radius();
            case Rectangle r -> r.width() * r.height();
            case Triangle  t -> 0.5 * t.base() * t.height();
            // No default needed — compiler verifies exhaustiveness!
        };
    }

    // ─────────────────────────────────────────────────────────
    // PATTERN MATCHING (Java 16+)
    // ─────────────────────────────────────────────────────────

    sealed interface JsonValue permits JsonString, JsonNumber, JsonBoolean, JsonNull, JsonArray {}
    record JsonString(String value)    implements JsonValue {}
    record JsonNumber(double value)    implements JsonValue {}
    record JsonBoolean(boolean value)  implements JsonValue {}
    record JsonNull()                  implements JsonValue {}
    record JsonArray(List<JsonValue> elements) implements JsonValue {}

    static String describe(JsonValue value) {
        return switch (value) {
            case JsonString  s -> "String: \"" + s.value() + "\"";
            case JsonNumber  n when n.value() > 0 -> "Positive number: " + n.value();
            case JsonNumber  n -> "Non-positive number: " + n.value();
            case JsonBoolean b -> "Boolean: " + b.value();
            case JsonNull    ignored -> "null";
            case JsonArray   a -> "Array with " + a.elements().size() + " elements";
        };
    }

    // ─────────────────────────────────────────────────────────
    // THE DATE/TIME API (Java 8+)
    // ─────────────────────────────────────────────────────────
    // Replaces the terrible java.util.Date and Calendar classes.
    // It's immutable, thread-safe, and actually makes sense.

    static void dateTimeExamples() {
        // LocalDate — date without time, without timezone
        LocalDate today = LocalDate.now();
        LocalDate birthday = LocalDate.of(1990, Month.MARCH, 15);
        LocalDate nextWeek = today.plusDays(7);
        LocalDate lastMonth = today.minusMonths(1);

        System.out.println(today);        // 2024-01-15
        System.out.println(birthday.getDayOfWeek()); // THURSDAY
        System.out.println(today.isAfter(birthday)); // true

        // LocalTime — time without date
        LocalTime now = LocalTime.now();
        LocalTime meeting = LocalTime.of(14, 30); // 2:30 PM
        System.out.println(meeting.isBefore(LocalTime.NOON)); // false

        // LocalDateTime — date AND time, no timezone
        LocalDateTime meeting2 = LocalDateTime.of(2024, 3, 15, 14, 30);
        LocalDateTime inTwoHours = LocalDateTime.now().plusHours(2);

        // ZonedDateTime — full timestamp with timezone (for APIs, storage)
        ZonedDateTime utcNow = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime nyNow  = ZonedDateTime.now(ZoneId.of("America/New_York"));
        ZonedDateTime tokyoNow = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"));

        // Instant — machine time (nanoseconds since epoch)
        Instant start = Instant.now();
        // ... do work ...
        Instant end = Instant.now();
        Duration elapsed = Duration.between(start, end);
        System.out.println("Elapsed: " + elapsed.toMillis() + "ms");

        // Period — human time between dates
        Period age = Period.between(birthday, today);
        System.out.println("Age: " + age.getYears() + " years");

        // DateTimeFormatter — parse and format
        java.time.format.DateTimeFormatter formatter =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String formatted = LocalDateTime.now().format(formatter);
        LocalDateTime parsed = LocalDateTime.parse("2024-03-15 14:30", formatter);
    }

    // ─────────────────────────────────────────────────────────
    // CUSTOM FUNCTIONAL INTERFACES
    // ─────────────────────────────────────────────────────────

    @FunctionalInterface
    interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;

        // Static helper to wrap checked exceptions
        static <T, R> Function<T, R> wrap(ThrowingFunction<T, R> f) {
            return t -> {
                try {
                    return f.apply(t);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }

    // Now you can use it in streams:
    static void checkedExceptionInStream() {
        List<String> paths = List.of("/tmp/a.txt", "/tmp/b.txt");
        // This normally doesn't compile because readFile throws IOException:
        // paths.stream().map(p -> readFile(p)) // ❌

        // With our wrapper:
        paths.stream()
            .map(ThrowingFunction.wrap(p -> new java.io.File(p).getName()))
            .forEach(System.out::println);
    }

    // ─────────────────────────────────────────────────────────
    // COMPLETABLEFUTURE — async programming
    // ─────────────────────────────────────────────────────────

    static void completableFutureExamples() throws Exception {
        // Async task that returns a value
        var future = java.util.concurrent.CompletableFuture
            .supplyAsync(() -> {
                // This runs on a thread pool
                return fetchUser(1L);
            })
            .thenApply(user -> user.toUpperCase())    // transform the result
            .thenAccept(System.out::println)           // consume the result
            .exceptionally(err -> { System.err.println("Error: " + err); return null; });

        // Run two tasks in parallel and combine
        var task1 = java.util.concurrent.CompletableFuture.supplyAsync(() -> "Hello");
        var task2 = java.util.concurrent.CompletableFuture.supplyAsync(() -> " World");
        var combined = task1.thenCombine(task2, (a, b) -> a + b);
        System.out.println(combined.get()); // "Hello World"

        // Wait for all of multiple futures
        var all = java.util.concurrent.CompletableFuture.allOf(task1, task2);
        all.join(); // waits for both
    }

    private static String fetchUser(long id) { return "user-" + id; }

    public static void main(String[] args) throws Exception {
        lambdaExamples();
        methodReferences();
        recordDemo();

        Shape circle = new Circle(5.0);
        System.out.println("Circle area: " + area(circle));

        dateTimeExamples();
        completableFutureExamples();
    }
}
