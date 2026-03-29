package fundamentals;

/**
 * ============================================================
 * 01 - JAVA FUNDAMENTALS: Types, Variables, Control Flow
 * ============================================================
 * Java is statically typed — every variable has a type known at
 * compile time. Get comfortable with this; it's your safety net.
 *
 * Everything in Java lives inside a class. Even this file.
 * That's not bureaucracy — it's structure.
 */
public class Fundamentals {

    // ─────────────────────────────────────────────────────────
    // PRIMITIVE TYPES — stored by value, not by reference
    // ─────────────────────────────────────────────────────────

    static void primitiveTypes() {
        // Integers
        byte   b  = 127;               // 8-bit,  -128 to 127
        short  s  = 32_000;            // 16-bit, ~32k range (note: _ for readability)
        int    i  = 2_147_483_647;     // 32-bit, the everyday integer
        long   l  = 9_223_372_036_854L; // 64-bit, note the L suffix

        // Floating point
        float  f  = 3.14f;             // 32-bit, note the f suffix
        double d  = 3.141592653589793; // 64-bit, default for decimals

        // Other primitives
        boolean flag = true;           // true or false, that's it
        char    c    = 'A';            // single Unicode character, 16-bit

        // ⚠️ GOTCHA: floating point is approximate!
        System.out.println(0.1 + 0.2); // prints 0.30000000000000004, not 0.3
        // Use BigDecimal for money or anything requiring exactness.

        // Integer overflow wraps silently — Java won't throw:
        int max = Integer.MAX_VALUE;
        System.out.println(max + 1); // prints -2147483648. Surprise!
    }

    // ─────────────────────────────────────────────────────────
    // REFERENCE TYPES — stored as a pointer to heap memory
    // ─────────────────────────────────────────────────────────

    static void referenceTypes() {
        String name = "Akito";    // String is a reference type
        String other = "Akito";   // might share the same pool entry

        // == compares REFERENCES (memory addresses), not content!
        System.out.println(name == other);           // true (string pool magic)
        System.out.println(name == new String("Akito")); // false (new heap object)
        System.out.println(name.equals("Akito"));    // ✅ always use equals() for strings

        // null — the absence of a reference
        String nullable = null;
        // nullable.length(); // ❌ NullPointerException at runtime!
        // Always null-check or use Optional (see modern-java section)

        // var — local type inference (Java 10+)
        var message = "Hello, World!"; // compiler infers String
        var number  = 42;              // compiler infers int
        // Use var when the type is obvious from the right side.
        // Don't use it when it obscures what the type actually is.
    }

    // ─────────────────────────────────────────────────────────
    // OPERATORS
    // ─────────────────────────────────────────────────────────

    static void operators() {
        int a = 10, b = 3;

        System.out.println(a + b);   // 13
        System.out.println(a - b);   // 7
        System.out.println(a * b);   // 30
        System.out.println(a / b);   // 3  ← integer division, truncates!
        System.out.println(a % b);   // 1  (modulo / remainder)

        // Integer vs float division:
        System.out.println(10 / 3);         // 3
        System.out.println(10.0 / 3);       // 3.3333...
        System.out.println((double) 10 / 3); // 3.3333... (explicit cast)

        // Compound assignment
        int x = 5;
        x += 3;   // x = 8
        x *= 2;   // x = 16
        x >>= 1;  // x = 8  (right bit shift — same as /2 for non-negatives)

        // Ternary — inline if/else
        int max = (a > b) ? a : b; // returns a if a>b, else b

        // String concatenation with +
        String result = "The answer is " + 42; // "The answer is 42"
        // Java auto-converts the int to String here
    }

    // ─────────────────────────────────────────────────────────
    // CONTROL FLOW
    // ─────────────────────────────────────────────────────────

    static void controlFlow() {
        // if / else if / else
        int score = 85;
        if      (score >= 90) System.out.println("A");
        else if (score >= 80) System.out.println("B");
        else if (score >= 70) System.out.println("C");
        else                  System.out.println("F");

        // switch statement (classic)
        String day = "MONDAY";
        switch (day) {
            case "MONDAY":
            case "TUESDAY":
            case "WEDNESDAY":
            case "THURSDAY":
            case "FRIDAY":
                System.out.println("Weekday");
                break;  // ⚠️ forgetting break = fall-through bug
            case "SATURDAY":
            case "SUNDAY":
                System.out.println("Weekend");
                break;
            default:
                System.out.println("Unknown");
        }

        // switch expression (Java 14+) — cleaner, no fall-through, returns a value
        String type = switch (day) {
            case "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY" -> "Weekday";
            case "SATURDAY", "SUNDAY" -> "Weekend";
            default -> throw new IllegalArgumentException("Unknown day: " + day);
        };
        System.out.println(type); // "Weekday"
    }

    // ─────────────────────────────────────────────────────────
    // LOOPS
    // ─────────────────────────────────────────────────────────

    static void loops() {
        // while
        int i = 0;
        while (i < 5) {
            System.out.print(i + " ");
            i++;
        }
        // Output: 0 1 2 3 4

        // do-while — executes body at least once
        int j = 0;
        do {
            System.out.print(j + " ");
            j++;
        } while (j < 5);

        // for loop
        for (int k = 0; k < 5; k++) {
            System.out.print(k + " ");
        }

        // enhanced for-each (works on arrays and Iterable)
        int[] numbers = {1, 2, 3, 4, 5};
        for (int n : numbers) {
            System.out.print(n + " ");
        }

        // break and continue
        for (int n = 0; n < 10; n++) {
            if (n == 3) continue; // skip 3
            if (n == 7) break;    // stop at 7
            System.out.print(n + " "); // 0 1 2 4 5 6
        }

        // labeled break — useful for nested loops
        outer:
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (row == 1 && col == 1) break outer; // exits BOTH loops
                System.out.println(row + "," + col);
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // ARRAYS
    // ─────────────────────────────────────────────────────────

    static void arrays() {
        // Declaration and initialisation
        int[] nums = new int[5];         // all zeros by default
        int[] primes = {2, 3, 5, 7, 11}; // array literal

        System.out.println(primes.length); // 5 (property, not method)
        System.out.println(primes[0]);     // 2
        // primes[10]; // ❌ ArrayIndexOutOfBoundsException

        // 2D array
        int[][] matrix = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        System.out.println(matrix[1][2]); // 6

        // Sorting
        java.util.Arrays.sort(primes);

        // Copying
        int[] copy = java.util.Arrays.copyOf(primes, primes.length);
        int[] range = java.util.Arrays.copyOfRange(primes, 1, 4); // {3,5,7}
    }

    // ─────────────────────────────────────────────────────────
    // STRINGS — a whole universe of their own
    // ─────────────────────────────────────────────────────────

    static void strings() {
        String s = "Hello, World!";

        // Common methods
        System.out.println(s.length());         // 13
        System.out.println(s.charAt(0));        // 'H'
        System.out.println(s.indexOf("World")); // 7
        System.out.println(s.substring(7));     // "World!"
        System.out.println(s.substring(7, 12)); // "World"
        System.out.println(s.toLowerCase());    // "hello, world!"
        System.out.println(s.toUpperCase());    // "HELLO, WORLD!"
        System.out.println(s.trim());           // removes leading/trailing whitespace
        System.out.println(s.replace("World", "Java")); // "Hello, Java!"
        System.out.println(s.contains("World")); // true
        System.out.println(s.startsWith("Hello")); // true
        System.out.println(s.endsWith("!"));       // true
        System.out.println(s.split(", "));       // ["Hello", "World!"]

        // String.format() — old style but you'll see it everywhere
        String formatted = String.format("Name: %s, Age: %d, Score: %.2f", "Akito", 35, 98.5);

        // Text blocks (Java 15+) — multiline strings without escapes
        String json = """
                {
                    "name": "Akito",
                    "age": 35
                }
                """;

        // StringBuilder — use when concatenating in a loop
        // String is IMMUTABLE — every + creates a new object
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("item").append(i).append(",");
        }
        String result = sb.toString();
        // If you did: String r = ""; for(...) r += "item" + i;
        // That's 100 new String objects. StringBuilder = one buffer.
    }

    public static void main(String[] args) {
        primitiveTypes();
        referenceTypes();
        operators();
        controlFlow();
        loops();
        arrays();
        strings();
    }
}
