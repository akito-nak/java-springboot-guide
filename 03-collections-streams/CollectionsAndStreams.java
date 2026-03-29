package collections;

import java.util.*;
import java.util.stream.*;
import java.util.function.*;

/**
 * ============================================================
 * 03 - COLLECTIONS & STREAMS
 * ============================================================
 * The Java Collections Framework is your Swiss Army knife.
 * Master this and you can solve almost any data manipulation
 * problem elegantly. The Streams API (Java 8+) transforms how
 * you think about processing data — think SQL, not for-loops.
 */
public class CollectionsAndStreams {

    // ─────────────────────────────────────────────────────────
    // THE COLLECTIONS HIERARCHY — choosing the right tool
    // ─────────────────────────────────────────────────────────
    //
    //  Collection
    //   ├── List     → ordered, allows duplicates
    //   │    ├── ArrayList   — fast random access, slow insert/delete in middle
    //   │    └── LinkedList  — fast insert/delete, slow random access
    //   ├── Set      → no duplicates
    //   │    ├── HashSet     — O(1) add/contains, NO order
    //   │    ├── LinkedHashSet — O(1) ops, insertion order preserved
    //   │    └── TreeSet     — sorted, O(log n) ops
    //   └── Queue    → FIFO or priority-based
    //        ├── ArrayDeque  — efficient stack AND queue
    //        └── PriorityQueue — min-heap by default
    //  Map (NOT a Collection!)
    //   ├── HashMap      — O(1) get/put, no order
    //   ├── LinkedHashMap — insertion order preserved
    //   └── TreeMap      — sorted by key, O(log n)

    static void listExamples() {
        System.out.println("=== LIST ===");

        // ArrayList — go-to for most use cases
        List<String> names = new ArrayList<>();
        names.add("Alice");
        names.add("Bob");
        names.add("Charlie");
        names.add(1, "Zara"); // insert at index 1

        System.out.println(names);          // [Alice, Zara, Bob, Charlie]
        System.out.println(names.get(0));   // Alice
        System.out.println(names.size());   // 4
        System.out.println(names.contains("Bob")); // true

        names.remove("Zara");       // remove by value
        names.remove(0);            // remove by index

        // Sorting
        Collections.sort(names);                          // natural order
        names.sort(Comparator.naturalOrder());            // same thing
        names.sort(Comparator.reverseOrder());            // reversed
        names.sort(Comparator.comparing(String::length)); // by length

        // Factory methods (Java 9+) — IMMUTABLE lists
        List<String> immutable = List.of("a", "b", "c");
        // immutable.add("d"); // ❌ UnsupportedOperationException

        // Mutable copy of an immutable list
        List<String> mutable = new ArrayList<>(immutable);

        // Converting array to list
        String[] arr = {"x", "y", "z"};
        List<String> fromArray = Arrays.asList(arr); // fixed size, backed by array
        List<String> flexible = new ArrayList<>(Arrays.asList(arr)); // truly flexible

        // Iterating
        for (String name : names) {
            System.out.print(name + " ");
        }
        names.forEach(name -> System.out.print(name + " ")); // lambda style
        names.forEach(System.out::println);                   // method reference style
    }

    static void mapExamples() {
        System.out.println("\n=== MAP ===");

        Map<String, Integer> scores = new HashMap<>();
        scores.put("Alice", 95);
        scores.put("Bob", 87);
        scores.put("Charlie", 92);
        scores.put("Alice", 98); // overwrites previous Alice entry

        System.out.println(scores.get("Alice"));          // 98
        System.out.println(scores.getOrDefault("Dave", 0)); // 0 — safe get

        // put only if key absent
        scores.putIfAbsent("Dave", 75);

        // Compute patterns
        scores.compute("Alice", (key, oldVal) -> oldVal == null ? 1 : oldVal + 10);
        scores.merge("Bob", 5, Integer::sum); // adds 5 to Bob's score

        // Check existence
        System.out.println(scores.containsKey("Alice"));   // true
        System.out.println(scores.containsValue(87));      // true

        // Iterating over entries — the right way
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
        scores.forEach((name, score) -> System.out.println(name + ": " + score));

        // Immutable map (Java 9+)
        Map<String, Integer> config = Map.of("timeout", 30, "retries", 3);
        // For more than 10 entries, use Map.ofEntries(Map.entry(...), ...)

        // LinkedHashMap — preserves insertion order
        Map<String, String> ordered = new LinkedHashMap<>();
        ordered.put("first",  "1st");
        ordered.put("second", "2nd");
        ordered.put("third",  "3rd");
        System.out.println(ordered); // {first=1st, second=2nd, third=3rd}

        // TreeMap — sorted by key
        Map<String, Integer> sorted = new TreeMap<>(scores);
        System.out.println(sorted.firstKey()); // alphabetically first
    }

    static void setAndQueueExamples() {
        System.out.println("\n=== SET & QUEUE ===");

        // HashSet — fastest, no order guaranteed
        Set<String> set = new HashSet<>(List.of("apple", "banana", "cherry", "apple"));
        System.out.println(set.size()); // 3 — duplicate removed

        // Set operations
        Set<Integer> a = new HashSet<>(Set.of(1, 2, 3, 4, 5));
        Set<Integer> b = new HashSet<>(Set.of(3, 4, 5, 6, 7));

        Set<Integer> intersection = new HashSet<>(a);
        intersection.retainAll(b); // {3, 4, 5}

        Set<Integer> union = new HashSet<>(a);
        union.addAll(b); // {1, 2, 3, 4, 5, 6, 7}

        Set<Integer> difference = new HashSet<>(a);
        difference.removeAll(b); // {1, 2}

        // ArrayDeque as a Stack (push/pop) or Queue (offer/poll)
        Deque<String> stack = new ArrayDeque<>();
        stack.push("first");
        stack.push("second");
        stack.push("third");
        System.out.println(stack.pop()); // "third" — LIFO

        Queue<String> queue = new ArrayDeque<>();
        queue.offer("first");
        queue.offer("second");
        queue.offer("third");
        System.out.println(queue.poll()); // "first" — FIFO

        // PriorityQueue — always removes the smallest element
        PriorityQueue<Integer> pq = new PriorityQueue<>();
        pq.offer(5); pq.offer(1); pq.offer(3);
        System.out.println(pq.poll()); // 1
        System.out.println(pq.poll()); // 3
    }

    // ─────────────────────────────────────────────────────────
    // STREAMS — functional pipeline processing
    // ─────────────────────────────────────────────────────────
    //
    // A Stream is a pipeline of operations on a data source.
    // Three phases: SOURCE → INTERMEDIATE OPS → TERMINAL OP
    //
    // Key properties:
    //  • Lazy — intermediate ops don't execute until a terminal op is called
    //  • Non-mutating — the source collection is never modified
    //  • Single-use — a stream can only be consumed once

    record Person(String name, int age, String city, double salary) {}

    static List<Person> samplePeople() {
        return List.of(
            new Person("Alice",   30, "New York",    95_000),
            new Person("Bob",     25, "Boston",      72_000),
            new Person("Charlie", 35, "New York",   120_000),
            new Person("Diana",   28, "Boston",      85_000),
            new Person("Eve",     42, "New York",   150_000),
            new Person("Frank",   31, "Chicago",     88_000),
            new Person("Grace",   26, "Chicago",     67_000)
        );
    }

    static void streamBasics() {
        System.out.println("\n=== STREAMS ===");
        List<Person> people = samplePeople();

        // filter — keep elements matching a predicate
        List<Person> adults = people.stream()
            .filter(p -> p.age() >= 30)
            .collect(Collectors.toList());

        // map — transform each element
        List<String> names = people.stream()
            .map(Person::name)
            .collect(Collectors.toList());
        System.out.println(names); // [Alice, Bob, Charlie, ...]

        // sorted — sort with a comparator
        List<Person> byAge = people.stream()
            .sorted(Comparator.comparing(Person::age))
            .collect(Collectors.toList());

        // distinct, limit, skip
        List<String> cities = people.stream()
            .map(Person::city)
            .distinct()        // unique cities
            .sorted()          // alphabetically
            .collect(Collectors.toList());
        System.out.println(cities); // [Boston, Chicago, New York]

        // reduce — aggregate all elements to one value
        double totalSalary = people.stream()
            .mapToDouble(Person::salary)
            .sum(); // shortcut for reduce
        System.out.println("Total salary: " + totalSalary);

        double avgAge = people.stream()
            .mapToInt(Person::age)
            .average()
            .orElse(0); // returns OptionalDouble
        System.out.println("Average age: " + avgAge);
    }

    static void streamCollectors() {
        System.out.println("\n=== COLLECTORS ===");
        List<Person> people = samplePeople();

        // Collect to specific types
        Set<String>        nameSet    = people.stream().map(Person::name).collect(Collectors.toSet());
        Map<String,Person> byName     = people.stream().collect(Collectors.toMap(Person::name, p -> p));
        String             joined     = people.stream().map(Person::name).collect(Collectors.joining(", "));
        System.out.println(joined); // Alice, Bob, Charlie, ...

        // groupingBy — like SQL GROUP BY
        Map<String, List<Person>> byCity = people.stream()
            .collect(Collectors.groupingBy(Person::city));
        byCity.forEach((city, residents) ->
            System.out.println(city + ": " + residents.stream().map(Person::name).collect(Collectors.joining(", ")))
        );

        // groupingBy with downstream collector
        Map<String, Long> countByCity = people.stream()
            .collect(Collectors.groupingBy(Person::city, Collectors.counting()));

        Map<String, Double> avgSalaryByCity = people.stream()
            .collect(Collectors.groupingBy(Person::city, Collectors.averagingDouble(Person::salary)));

        // partitioningBy — special case of groupingBy, splits into true/false
        Map<Boolean, List<Person>> overThirty = people.stream()
            .collect(Collectors.partitioningBy(p -> p.age() >= 30));
        System.out.println("Over 30: " + overThirty.get(true).stream().map(Person::name).toList());
        System.out.println("Under 30: " + overThirty.get(false).stream().map(Person::name).toList());

        // Collecting statistics
        IntSummaryStatistics ageStats = people.stream()
            .collect(Collectors.summarizingInt(Person::age));
        System.out.println("Age stats: min=" + ageStats.getMin() + ", max=" + ageStats.getMax() +
            ", avg=" + ageStats.getAverage());
    }

    static void streamAdvanced() {
        System.out.println("\n=== ADVANCED STREAMS ===");
        List<Person> people = samplePeople();

        // flatMap — flatten nested collections
        List<List<Integer>> nested = List.of(List.of(1,2,3), List.of(4,5), List.of(6));
        List<Integer> flat = nested.stream()
            .flatMap(Collection::stream) // flatten
            .collect(Collectors.toList());
        System.out.println(flat); // [1, 2, 3, 4, 5, 6]

        // peek — for debugging pipelines (doesn't change the stream)
        List<Person> result = people.stream()
            .filter(p -> p.salary() > 80_000)
            .peek(p -> System.out.println("After filter: " + p.name()))
            .sorted(Comparator.comparing(Person::salary).reversed())
            .peek(p -> System.out.println("After sort: " + p.name()))
            .limit(3)
            .collect(Collectors.toList());

        // anyMatch, allMatch, noneMatch — short-circuit terminal ops
        boolean anyNYC    = people.stream().anyMatch(p -> p.city().equals("New York"));
        boolean allAdults = people.stream().allMatch(p -> p.age() >= 18);
        boolean noneZero  = people.stream().noneMatch(p -> p.salary() == 0);

        // findFirst / findAny
        Optional<Person> firstNYC = people.stream()
            .filter(p -> p.city().equals("New York"))
            .findFirst();
        firstNYC.ifPresent(p -> System.out.println("First NYC person: " + p.name()));

        // min and max
        Optional<Person> youngest = people.stream().min(Comparator.comparing(Person::age));
        Optional<Person> richest  = people.stream().max(Comparator.comparing(Person::salary));

        // Parallel streams — use for CPU-intensive operations on large datasets
        // Be careful: not always faster, and has caveats with shared state
        long count = people.parallelStream()
            .filter(p -> p.salary() > 80_000)
            .count();

        // Stream.generate and Stream.iterate — infinite streams
        List<Integer> randoms = Stream.generate(() -> (int)(Math.random() * 100))
            .limit(10)
            .collect(Collectors.toList());

        List<Integer> fibonacci = Stream.iterate(new int[]{0, 1}, f -> new int[]{f[1], f[0] + f[1]})
            .limit(10)
            .map(f -> f[0])
            .collect(Collectors.toList());
        System.out.println("Fibonacci: " + fibonacci);
    }

    // ─────────────────────────────────────────────────────────
    // OPTIONAL — banish NullPointerException
    // ─────────────────────────────────────────────────────────

    static Optional<Person> findByName(List<Person> people, String name) {
        return people.stream()
            .filter(p -> p.name().equals(name))
            .findFirst();
    }

    static void optionalExamples() {
        System.out.println("\n=== OPTIONAL ===");
        List<Person> people = samplePeople();

        Optional<Person> found = findByName(people, "Alice");

        // BAD: getting value without checking
        // Person p = found.get(); // throws NoSuchElementException if empty!

        // GOOD patterns:
        found.ifPresent(p -> System.out.println("Found: " + p.name()));

        String name = found.map(Person::name).orElse("Unknown");
        double salary = found.map(Person::salary).orElse(0.0);

        // orElseGet — lazy evaluation (better for expensive defaults)
        Person person = found.orElseGet(() -> new Person("Default", 0, "N/A", 0));

        // orElseThrow
        Person alice = found.orElseThrow(() -> new RuntimeException("Alice not found"));

        // Chaining Optional
        Optional<Person> maybeEve = findByName(people, "Eve");
        String result = maybeEve
            .filter(p -> p.salary() > 100_000) // only if salary > 100k
            .map(p -> p.name() + " earns big")
            .orElse("Not found or not high earner");
        System.out.println(result); // "Eve earns big"

        // flatMap for nested Optionals
        Optional<Optional<String>> nested2 = Optional.of(Optional.of("hello"));
        Optional<String> flat2 = nested2.flatMap(o -> o); // unwraps the nesting
    }

    public static void main(String[] args) {
        listExamples();
        mapExamples();
        setAndQueueExamples();
        streamBasics();
        streamCollectors();
        streamAdvanced();
        optionalExamples();
    }
}
