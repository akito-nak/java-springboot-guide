package data;

// ============================================================
// 07 - SPRING DATA JPA
// ============================================================
// Spring Data JPA = Spring's abstraction over JPA (Java Persistence API),
// which is itself an abstraction over Hibernate, which writes SQL for you.
//
// The magic: extend JpaRepository<Entity, ID> and you get full
// CRUD + pagination + sorting — zero implementation code.

import jakarta.persistence.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

// ─────────────────────────────────────────────────────────────
// ENTITIES — your domain objects, mapped to database tables
// ─────────────────────────────────────────────────────────────

@Entity
@Table(name = "users",
    indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_status", columnList = "status")
    }
)
class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB auto-increment
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING) // store enum as "ACTIVE", not 0/1
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // One user can have many orders — the "one" side
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();

    // Many users can belong to many roles
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @PrePersist  // called before INSERT
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    @PreUpdate   // called before UPDATE
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    // Constructors, getters, setters
    protected User() {} // JPA requires no-arg constructor

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Getters and setters omitted for brevity — in real code use Lombok @Data
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public UserStatus getStatus() { return status; }
}

enum UserStatus { ACTIVE, INACTIVE, SUSPENDED }

@Entity
@Table(name = "orders")
class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)           // many orders → one user
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal total;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }

    protected Order() {}
    public Order(User user, java.math.BigDecimal total) {
        this.user = user;
        this.total = total;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public java.math.BigDecimal getTotal() { return total; }
}

enum OrderStatus { PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED }

@Entity @Table(name = "order_items")
class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private String productName;
    private int quantity;
    private java.math.BigDecimal price;

    protected OrderItem() {}
}

@Entity @Table(name = "roles")
class Role {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    protected Role() {}
    public Role(String name) { this.name = name; }
    public String getName() { return name; }
}

// ─────────────────────────────────────────────────────────────
// REPOSITORIES — zero implementation, infinite power
// ─────────────────────────────────────────────────────────────

// Extending JpaRepository gives you for FREE:
//   save(entity), saveAll(list), findById(id), findAll(),
//   findAll(pageable), findAll(sort), count(), delete*(...)
//   existsById(id), flush(), saveAndFlush(entity)

@Repository
interface UserRepository extends JpaRepository<User, Long> {

    // ── Derived query methods ───────────────────────────────
    // Spring READS the method name and generates SQL automatically!
    // The naming convention: find[By|Top|First][Property][Operator]

    Optional<User> findByEmail(String email);

    List<User> findByStatus(UserStatus status);

    List<User> findByNameContainingIgnoreCase(String nameFragment);

    boolean existsByEmail(String email);

    long countByStatus(UserStatus status);

    // And/Or combinations
    List<User> findByStatusAndCreatedAtAfter(UserStatus status, LocalDateTime after);
    List<User> findByNameOrEmail(String name, String email);

    // Sorting and limiting
    List<User> findTop10ByStatusOrderByCreatedAtDesc(UserStatus status);
    Optional<User> findFirstByStatusOrderByCreatedAtAsc(UserStatus status);

    // ── @Query — write JPQL (or SQL) directly ───────────────
    // JPQL uses entity/field names, not table/column names

    @Query("SELECT u FROM User u WHERE u.email LIKE %:domain%")
    List<User> findByEmailDomain(@Param("domain") String domain);

    // Native SQL (use sparingly — ties you to the DB)
    @Query(value = "SELECT * FROM users WHERE EXTRACT(YEAR FROM created_at) = :year",
           nativeQuery = true)
    List<User> findUsersCreatedInYear(@Param("year") int year);

    // ── Projections — return only what you need ──────────────
    // Instead of loading full entities when you only need a few fields

    @Query("SELECT new data.UserSummary(u.id, u.name, u.email) FROM User u WHERE u.status = :status")
    List<UserSummary> findSummaryByStatus(@Param("status") UserStatus status);

    // Interface projection — even simpler
    List<UserNameAndEmail> findByStatusNot(UserStatus status);

    // ── Modifying queries ─────────────────────────────────────
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.status = :status WHERE u.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") UserStatus status);

    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.status = :status AND u.createdAt < :before")
    int deleteInactiveUsersBefore(@Param("status") UserStatus status,
                                  @Param("before") LocalDateTime before);

    // ── Pagination ────────────────────────────────────────────
    Page<User> findByStatus(UserStatus status, Pageable pageable);
}

// Projection interfaces — Spring generates a proxy that returns only these fields
interface UserNameAndEmail {
    Long getId();
    String getName();
    String getEmail();
}

// DTO for constructor expression
record UserSummary(Long id, String name, String email) {}

@Repository
interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
    // JOIN FETCH = load the related collection in ONE query (avoids N+1 problem)

    // Aggregation
    @Query("SELECT SUM(o.total) FROM Order o WHERE o.user.id = :userId")
    Optional<java.math.BigDecimal> sumTotalByUserId(@Param("userId") Long userId);
}

// ─────────────────────────────────────────────────────────────
// SERVICE LAYER WITH TRANSACTIONS
// ─────────────────────────────────────────────────────────────
// @Transactional: all operations in this method succeed or ALL roll back.
// Use it at the service layer, not repository layer.

@Service
@Transactional(readOnly = true)  // default: read-only (better performance)
class UserService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    UserService(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public Page<User> findActiveUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return userRepository.findByStatus(UserStatus.ACTIVE, pageable);
    }

    @Transactional  // overrides readOnly = true — this method WRITES
    public User createUser(String name, String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }
        return userRepository.save(new User(name, email));
    }

    @Transactional
    public Order placeOrder(Long userId, java.math.BigDecimal total) {
        User user = findById(userId);
        Order order = new Order(user, total);
        // If ANY of this throws an exception, the whole transaction rolls back
        Order saved = orderRepository.save(order);
        // updateInventory(...)  — all or nothing
        // sendConfirmationEmail(...) — ❌ don't call external services in a transaction!
        return saved;
    }

    // ── PAGINATION RESPONSE ───────────────────────────────────
    // How to work with Page<T>:
    void paginationDemo() {
        Pageable first    = PageRequest.of(0, 20, Sort.by("name"));
        Page<User> page   = userRepository.findByStatus(UserStatus.ACTIVE, first);

        System.out.println("Total users: "  + page.getTotalElements());
        System.out.println("Total pages: "  + page.getTotalPages());
        System.out.println("Current page: " + page.getNumber());
        System.out.println("Has next: "     + page.hasNext());
        System.out.println("Content: "      + page.getContent()); // List<User>
    }
}

// ─────────────────────────────────────────────────────────────
// THE N+1 PROBLEM — every JPA developer gets burned by this
// ─────────────────────────────────────────────────────────────
// If you load 100 orders and access order.getUser() on each one,
// JPA fires 100 separate SELECT queries to load each user.
// That's 1 (load orders) + 100 (load users) = 101 queries. Bad.
//
// Solutions:
//   1. JOIN FETCH in JPQL:  SELECT o FROM Order o JOIN FETCH o.user
//   2. @EntityGraph:        declarative version of JOIN FETCH
//   3. @BatchSize:          loads related entities in batches
//   4. Projections:         only load what you need

@Repository
interface OrderRepositoryWithEntityGraph extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"user", "items"}) // load these eagerly
    List<Order> findByStatus(OrderStatus status);

    // Alternatively, define a named entity graph on the entity:
    // @NamedEntityGraph(name = "Order.withUser", attributeNodes = @NamedAttributeNode("user"))
}
