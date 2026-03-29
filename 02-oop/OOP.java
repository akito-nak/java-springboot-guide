package oop;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * 02 - OBJECT-ORIENTED PROGRAMMING
 * ============================================================
 * The four pillars: Encapsulation, Inheritance, Polymorphism, Abstraction.
 * Java was built for OOP. Understanding these deeply is non-negotiable.
 *
 * We'll build a mini bank account system to illustrate everything.
 */

// ─────────────────────────────────────────────────────────────
// ENCAPSULATION — hide data, expose behaviour
// ─────────────────────────────────────────────────────────────
// Make fields private. Control access through getters/setters.
// This lets you enforce invariants (e.g., balance can't go negative).

class BankAccount {
    // private — only this class can touch these directly
    private final String accountNumber;  // final = assigned once, never changed
    private final String owner;
    private double balance;
    private final List<String> transactions = new ArrayList<>();

    // Constructor
    public BankAccount(String accountNumber, String owner, double initialBalance) {
        if (initialBalance < 0) throw new IllegalArgumentException("Initial balance cannot be negative");
        this.accountNumber = accountNumber;
        this.owner = owner;
        this.balance = initialBalance;
        transactions.add("Account opened with $" + initialBalance);
    }

    // Public behaviour — the API you expose
    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive");
        balance += amount;
        transactions.add("Deposit: +" + amount);
    }

    public void withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Withdrawal amount must be positive");
        if (amount > balance) throw new IllegalStateException("Insufficient funds");
        balance -= amount;
        transactions.add("Withdrawal: -" + amount);
    }

    // Getters — read-only access to private state
    public String getAccountNumber() { return accountNumber; }
    public String getOwner()         { return owner; }
    public double getBalance()       { return balance; }
    public List<String> getTransactions() { return List.copyOf(transactions); } // defensive copy

    // Override toString for meaningful output
    @Override
    public String toString() {
        return String.format("BankAccount[%s, owner=%s, balance=%.2f]",
            accountNumber, owner, balance);
    }

    // Override equals and hashCode when objects represent the same value
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;          // same reference
        if (!(o instanceof BankAccount)) return false;
        BankAccount other = (BankAccount) o;
        return accountNumber.equals(other.accountNumber);
    }

    @Override
    public int hashCode() { return accountNumber.hashCode(); }
}

// ─────────────────────────────────────────────────────────────
// INHERITANCE — build on existing classes
// ─────────────────────────────────────────────────────────────
// Prefer composition over inheritance (see patterns section),
// but inheritance is crucial to understand.

class SavingsAccount extends BankAccount {
    private double interestRate; // additional field

    public SavingsAccount(String accountNumber, String owner,
                          double initialBalance, double interestRate) {
        super(accountNumber, owner, initialBalance); // call parent constructor first
        this.interestRate = interestRate;
    }

    // New behaviour — only on SavingsAccount
    public void applyInterest() {
        double interest = getBalance() * interestRate;
        deposit(interest); // calls parent's deposit method
    }

    // Override withdraw to add a minimum balance constraint
    @Override
    public void withdraw(double amount) {
        double minimumBalance = 100.0;
        if (getBalance() - amount < minimumBalance) {
            throw new IllegalStateException(
                "Savings accounts must maintain a $" + minimumBalance + " minimum balance"
            );
        }
        super.withdraw(amount); // call parent implementation
    }

    public double getInterestRate() { return interestRate; }
}

// ─────────────────────────────────────────────────────────────
// ABSTRACT CLASSES — partial implementation + contract
// ─────────────────────────────────────────────────────────────
// Use when you have shared implementation code AND a contract.
// Cannot be instantiated directly.

abstract class Shape {
    protected String color;

    public Shape(String color) {
        this.color = color;
    }

    // Abstract method — every Shape MUST implement this
    public abstract double area();
    public abstract double perimeter();

    // Concrete method — shared across all shapes
    public String describe() {
        return String.format("%s %s: area=%.2f, perimeter=%.2f",
            color, getClass().getSimpleName(), area(), perimeter());
    }
}

class Circle extends Shape {
    private double radius;

    public Circle(String color, double radius) {
        super(color);
        this.radius = radius;
    }

    @Override public double area()      { return Math.PI * radius * radius; }
    @Override public double perimeter() { return 2 * Math.PI * radius; }
}

class Rectangle extends Shape {
    private double width, height;

    public Rectangle(String color, double width, double height) {
        super(color);
        this.width = width;
        this.height = height;
    }

    @Override public double area()      { return width * height; }
    @Override public double perimeter() { return 2 * (width + height); }
}

// ─────────────────────────────────────────────────────────────
// INTERFACES — pure contracts, multiple inheritance of type
// ─────────────────────────────────────────────────────────────
// A class can only extend ONE class but implement MANY interfaces.
// Think of interfaces as capabilities or roles.

interface Printable {
    void print(); // implicitly public and abstract
}

interface Saveable {
    void save(String filename);
    // Default method — provides a default implementation (Java 8+)
    default void saveToTemp() {
        save("/tmp/default.dat");
    }
}

interface Auditable {
    // Static method on an interface (Java 8+)
    static String timestamp() {
        return java.time.LocalDateTime.now().toString();
    }
    void audit(String action);
}

// A class can implement multiple interfaces
class Document implements Printable, Saveable, Auditable {
    private String content;

    public Document(String content) { this.content = content; }

    @Override public void print() { System.out.println(content); }
    @Override public void save(String filename) { System.out.println("Saving to " + filename); }
    @Override public void audit(String action) {
        System.out.println("[AUDIT] " + Auditable.timestamp() + " - " + action);
    }
}

// ─────────────────────────────────────────────────────────────
// POLYMORPHISM — one interface, many implementations
// ─────────────────────────────────────────────────────────────
// The ability to treat objects of different types uniformly
// through a shared parent type or interface.

class PolymorphismDemo {
    // This method works with ANY Shape — it doesn't care about the subtype
    public static void printShapeInfo(Shape shape) {
        System.out.println(shape.describe()); // calls the right area() at runtime
    }

    // Works with anything that's Printable
    public static void printAll(List<Printable> items) {
        items.forEach(Printable::print);
    }

    public static void main(String[] args) {
        // Polymorphic collection — list of Shapes, not a specific shape
        List<Shape> shapes = List.of(
            new Circle("red", 5.0),
            new Rectangle("blue", 4.0, 6.0),
            new Circle("green", 3.0)
        );

        // Each call to describe() dispatches to the right subclass at RUNTIME
        // This is called dynamic dispatch or virtual method dispatch
        shapes.forEach(PolymorphismDemo::printShapeInfo);

        // instanceof check + pattern matching (Java 16+)
        for (Shape s : shapes) {
            if (s instanceof Circle c) {
                // c is already typed as Circle here — no cast needed!
                System.out.println("Circle with radius access: " + c.area());
            } else if (s instanceof Rectangle r) {
                System.out.println("Rectangle: " + r.describe());
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// INNER CLASSES & ANONYMOUS CLASSES
// ─────────────────────────────────────────────────────────────

class OuterClass {
    private int value = 42;

    // Non-static inner class — has access to outer class members
    class Inner {
        public void display() {
            System.out.println("Outer value: " + value); // can access outer private!
        }
    }

    // Static nested class — no access to outer instance
    static class StaticNested {
        public void doSomething() {
            System.out.println("I'm a static nested class");
        }
    }

    // Local class — defined inside a method
    public void method() {
        class Local {
            public void speak() { System.out.println("I'm local"); }
        }
        new Local().speak();
    }

    // Anonymous class — define and instantiate on the spot
    // (Less common now that we have lambdas, but you'll see this in legacy code)
    public Runnable createRunnable() {
        return new Runnable() { // anonymous class implementing Runnable
            @Override
            public void run() {
                System.out.println("Anonymous class running");
            }
        };
    }
}

// ─────────────────────────────────────────────────────────────
// ENUMS — more powerful than you think
// ─────────────────────────────────────────────────────────────

enum Planet {
    MERCURY(3.303e+23, 2.4397e6),
    VENUS  (4.869e+24, 6.0518e6),
    EARTH  (5.976e+24, 6.37814e6),
    MARS   (6.421e+23, 3.3972e6);

    private final double mass;   // in kilograms
    private final double radius; // in meters
    static final double G = 6.67300E-11;

    Planet(double mass, double radius) {
        this.mass = mass;
        this.radius = radius;
    }

    // Enums can have methods!
    double surfaceGravity() { return G * mass / (radius * radius); }
    double surfaceWeight(double otherMass) { return otherMass * surfaceGravity(); }
}

// Enum with abstract method — each constant has its own implementation
enum Operation {
    PLUS("+")  { @Override public double apply(double x, double y) { return x + y; } },
    MINUS("-") { @Override public double apply(double x, double y) { return x - y; } },
    TIMES("*") { @Override public double apply(double x, double y) { return x * y; } },
    DIVIDE("/") { @Override public double apply(double x, double y) { return x / y; } };

    private final String symbol;
    Operation(String symbol) { this.symbol = symbol; }

    public abstract double apply(double x, double y);
    @Override public String toString() { return symbol; }
}
