package rest;

// ============================================================
// 09 - BUILDING REST APIs WITH SPRING
// ============================================================
// This is where Spring Boot shines hardest. Building a production-grade
// REST API with proper request handling, validation, error responses,
// and pagination takes surprisingly little code.

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.validation.annotation.*;
import jakarta.validation.*;
import jakarta.validation.constraints.*;
import java.util.*;
import java.time.*;

// ─────────────────────────────────────────────────────────────
// DTOs — Data Transfer Objects
// ─────────────────────────────────────────────────────────────
// NEVER expose your @Entity directly over the API.
// Use DTOs to control exactly what goes in and out.

record CreateUserRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be 2–100 characters")
    String name,

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,

    @Min(value = 18, message = "Must be at least 18 years old")
    @Max(value = 120)
    Integer age
) {}

record UpdateUserRequest(
    @Size(min = 2, max = 100) String name,  // all optional for updates
    @Email String email
) {}

record UserResponse(
    Long id,
    String name,
    String email,
    String status,
    LocalDateTime createdAt
) {}

record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious
) {}

record ErrorResponse(
    int status,
    String error,
    String message,
    LocalDateTime timestamp,
    Map<String, String> fieldErrors   // for validation failures
) {}

// ─────────────────────────────────────────────────────────────
// MAIN REST CONTROLLER
// ─────────────────────────────────────────────────────────────

@RestController                         // @Controller + auto-serialise return values to JSON
@RequestMapping("/api/v1/users")        // base path for all methods in this class
@Validated                              // enable method-level validation
class UserController {

    private final UserService userService; // injected

    UserController(UserService userService) {
        this.userService = userService;
    }

    // GET /api/v1/users?page=0&size=20&sort=name,asc&status=ACTIVE
    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> listUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String status) {

        PageResponse<UserResponse> response = userService.listUsers(page, size, status);
        return ResponseEntity.ok(response);
    }

    // GET /api/v1/users/42
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    // POST /api/v1/users
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {  // @Valid triggers Bean Validation

        UserResponse created = userService.createUser(request);
        URI location = URI.create("/api/v1/users/" + created.id());
        return ResponseEntity
            .created(location)   // 201 Created with Location header
            .body(created);
    }

    // PUT /api/v1/users/42  (full replacement)
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> replaceUser(
            @PathVariable Long id,
            @Valid @RequestBody CreateUserRequest request) {

        return ResponseEntity.ok(userService.replaceUser(id, request));
    }

    // PATCH /api/v1/users/42  (partial update)
    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request) {  // no @Valid — partial update

        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    // DELETE /api/v1/users/42
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();  // 204 No Content
    }

    // GET /api/v1/users/42/orders
    @GetMapping("/{id}/orders")
    public ResponseEntity<List<OrderResponse>> getUserOrders(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserOrders(id));
    }

    // POST /api/v1/users/search  (complex queries)
    @PostMapping("/search")
    public ResponseEntity<PageResponse<UserResponse>> searchUsers(
            @RequestBody UserSearchRequest criteria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(userService.search(criteria, page, size));
    }
}

// ─────────────────────────────────────────────────────────────
// GLOBAL EXCEPTION HANDLER
// ─────────────────────────────────────────────────────────────
// Centralises error handling — no try/catch in controllers!
// Every exception type maps to the right HTTP status + body.

@RestControllerAdvice  // applies to ALL @RestController classes
class GlobalExceptionHandler {

    // Validation errors (from @Valid)
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponse(400, "Validation Failed", "One or more fields are invalid",
                LocalDateTime.now(), fieldErrors)
        );
    }

    // Custom business exceptions
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            new ErrorResponse(404, "Not Found", ex.getMessage(),
                LocalDateTime.now(), null)
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            new ErrorResponse(409, "Conflict", ex.getMessage(),
                LocalDateTime.now(), null)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArg(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponse(400, "Bad Request", ex.getMessage(),
                LocalDateTime.now(), null)
        );
    }

    // Catch-all — prevents stack traces leaking to clients
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        // Log the full exception internally
        System.err.println("Unexpected error: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ErrorResponse(500, "Internal Server Error", "An unexpected error occurred",
                LocalDateTime.now(), null)
        );
    }
}

// ─────────────────────────────────────────────────────────────
// CUSTOM EXCEPTIONS
// ─────────────────────────────────────────────────────────────

class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " not found with id: " + id);
    }
}

class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) { super(message); }
}

// ─────────────────────────────────────────────────────────────
// REQUEST PARAMETER HANDLING
// ─────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/v1/products")
class ProductController {

    // Path variables
    @GetMapping("/{category}/{id}")
    public String getProduct(
            @PathVariable String category,
            @PathVariable @Min(1) Long id) {
        return "Product " + id + " in " + category;
    }

    // Query parameters — multiple examples
    @GetMapping("/search")
    public String searchProducts(
            @RequestParam String query,
            @RequestParam(defaultValue = "price") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return String.format("Searching '%s' sorted by %s %s", query, sortBy, direction);
    }

    // Request headers
    @GetMapping("/featured")
    public String getFeatured(
            @RequestHeader("Authorization") String authHeader,
            @RequestHeader(value = "Accept-Language", defaultValue = "en") String lang) {
        return "Featured products for language: " + lang;
    }

    // Matrix variables (rare but good to know)
    // GET /products/filter;color=red,blue;size=M,L
    @GetMapping("/filter/{specs}")
    public String filterByMatrix(
            @MatrixVariable List<String> color,
            @MatrixVariable List<String> size) {
        return "Filtering: colors=" + color + ", sizes=" + size;
    }
}

// ─────────────────────────────────────────────────────────────
// CUSTOM VALIDATOR
// ─────────────────────────────────────────────────────────────

@java.lang.annotation.Target({java.lang.annotation.ElementType.FIELD})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordStrengthValidator.class)
@interface StrongPassword {
    String message() default "Password must contain uppercase, lowercase, number, and special character";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

class PasswordStrengthValidator implements ConstraintValidator<StrongPassword, String> {
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) return false;
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$");
    }
}

// ─────────────────────────────────────────────────────────────
// PLACEHOLDER TYPES for compilation
// ─────────────────────────────────────────────────────────────
interface UserService {
    PageResponse<UserResponse> listUsers(int page, int size, String status);
    UserResponse getUser(Long id);
    UserResponse createUser(CreateUserRequest request);
    UserResponse replaceUser(Long id, CreateUserRequest request);
    UserResponse updateUser(Long id, UpdateUserRequest request);
    void deleteUser(Long id);
    List<OrderResponse> getUserOrders(Long id);
    PageResponse<UserResponse> search(UserSearchRequest criteria, int page, int size);
}
record OrderResponse(Long id, String status, java.math.BigDecimal total) {}
record UserSearchRequest(String name, String email, String status) {}
