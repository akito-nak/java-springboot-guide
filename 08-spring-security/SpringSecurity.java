package security;

// ============================================================
// 08 - SPRING SECURITY
// ============================================================
// Spring Security handles authentication ("who are you?") and
// authorisation ("what can you do?"). It integrates deeply with
// the Spring ecosystem and adds security to every layer of the stack.
//
// The core filter chain: every HTTP request passes through a chain
// of filters before reaching your controller. Security lives in that chain.
//
// pom.xml dependency: spring-boot-starter-security
// Once added, ALL endpoints require authentication by default.

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.*;
import org.springframework.security.config.annotation.web.configuration.*;
import org.springframework.security.config.http.*;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.*;
import org.springframework.security.access.prepost.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.filter.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.util.*;
import java.util.Date;

// ─────────────────────────────────────────────────────────────
// USER DETAILS SERVICE — bridge between Spring Security and your DB
// ─────────────────────────────────────────────────────────────

@Service
class CustomUserDetailsService implements UserDetailsService {
    // Spring Security calls loadUserByUsername when authenticating
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // In real code: query your user repository
        // User dbUser = userRepository.findByEmail(email)
        //     .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // For demo:
        if (!email.equals("admin@example.com")) {
            throw new UsernameNotFoundException("User not found: " + email);
        }

        // Build a Spring Security UserDetails from your domain User
        return org.springframework.security.core.userdetails.User.builder()
            .username(email)
            .password("$2a$10$...bcrypt_hash_here...")  // BCrypt-hashed
            .roles("USER", "ADMIN")
            // or: .authorities("ROLE_USER", "ROLE_ADMIN", "READ_USERS", "WRITE_USERS")
            .build();
    }
}

// ─────────────────────────────────────────────────────────────
// SECURITY CONFIGURATION
// ─────────────────────────────────────────────────────────────

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // enables @PreAuthorize, @PostAuthorize on methods
class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtFilter;

    SecurityConfig(CustomUserDetailsService userDetailsService,
                   JwtAuthenticationFilter jwtFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless REST APIs (JWTs handle this)
            .csrf(csrf -> csrf.disable())

            // Stateless session — no server-side session (we use JWTs)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Define which requests need authentication
            .authorizeHttpRequests(auth -> auth
                // Public endpoints — no auth required
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/products/**").permitAll()

                // Role-based access
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/**").hasRole("ADMIN")

                // Authority-based access
                .requestMatchers("/api/v1/reports/**").hasAuthority("VIEW_REPORTS")

                // Everything else needs authentication
                .anyRequest().authenticated()
            )

            // Add our JWT filter before Spring's username/password filter
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with strength 12 — the standard for password hashing
        // Higher = slower (protects against brute force)
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}

// ─────────────────────────────────────────────────────────────
// JWT SERVICE — create and validate tokens
// ─────────────────────────────────────────────────────────────
// JWT (JSON Web Token) = base64(header).base64(claims).signature
// The signature ensures the token wasn't tampered with.
// Add to pom.xml: io.jsonwebtoken:jjwt-api, jjwt-impl, jjwt-jackson

@Service
class JwtService {
    // In production, load this from application.yml or a secret manager!
    private static final String SECRET_KEY = "your-256-bit-secret-key-here-must-be-long-enough";
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24 hours

    private javax.crypto.SecretKey getSigningKey() {
        byte[] keyBytes = java.util.Base64.getDecoder().decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
            .claims(extraClaims)
            .subject(userDetails.getUsername())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
            .signWith(getSigningKey())
            .compact();
    }

    public String generateRefreshToken(UserDetails userDetails) {
        long refreshExpiry = 7 * 24 * 60 * 60 * 1000L; // 7 days
        return Jwts.builder()
            .subject(userDetails.getUsername())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + refreshExpiry))
            .signWith(getSigningKey())
            .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claimsResolver.apply(claims);
    }
}

// ─────────────────────────────────────────────────────────────
// JWT AUTHENTICATION FILTER
// ─────────────────────────────────────────────────────────────
// This filter runs on EVERY request. It:
//   1. Reads the Authorization header
//   2. Validates the JWT
//   3. Sets the authentication in the SecurityContext
// Once set, Spring Security knows the request is authenticated.

@Component
class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService uds) {
        this.jwtService = jwtService;
        this.userDetailsService = uds;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, java.io.IOException {

        final String authHeader = request.getHeader("Authorization");

        // No token? Pass through — Spring Security will reject if auth is required
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7); // strip "Bearer "

        try {
            final String username = jwtService.extractUsername(jwt);

            // Only authenticate if not already authenticated
            if (username != null &&
                org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Create authentication token and set it in the context
                    var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                        new org.springframework.security.web.authentication.WebAuthenticationDetailsSource()
                            .buildDetails(request)
                    );
                    org.springframework.security.core.context.SecurityContextHolder
                        .getContext()
                        .setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Invalid token — just don't set authentication, Spring Security handles the rest
        }

        filterChain.doFilter(request, response);
    }
}

// ─────────────────────────────────────────────────────────────
// AUTH CONTROLLER
// ─────────────────────────────────────────────────────────────

record LoginRequest(String email, String password) {}
record LoginResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {}
record RegisterRequest(String name, String email, String password) {}

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {
    private final AuthenticationManager authManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    AuthController(AuthenticationManager authManager,
                   CustomUserDetailsService userDetailsService,
                   JwtService jwtService,
                   PasswordEncoder passwordEncoder) {
        this.authManager = authManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public org.springframework.http.ResponseEntity<LoginResponse> login(
            @org.springframework.web.bind.annotation.RequestBody LoginRequest request) {

        // This throws BadCredentialsException if auth fails
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetails user = userDetailsService.loadUserByUsername(request.email());
        String accessToken  = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return org.springframework.http.ResponseEntity.ok(
            new LoginResponse(accessToken, refreshToken, "Bearer", 86400)
        );
    }

    @PostMapping("/register")
    public org.springframework.http.ResponseEntity<LoginResponse> register(
            @org.springframework.web.bind.annotation.RequestBody RegisterRequest request) {

        // Hash the password before storing
        String hashedPassword = passwordEncoder.encode(request.password());
        // userRepository.save(new User(request.name(), request.email(), hashedPassword));

        UserDetails user = userDetailsService.loadUserByUsername(request.email());
        return org.springframework.http.ResponseEntity.ok(
            new LoginResponse(jwtService.generateToken(user),
                jwtService.generateRefreshToken(user), "Bearer", 86400)
        );
    }
}

// ─────────────────────────────────────────────────────────────
// METHOD-LEVEL SECURITY — @PreAuthorize
// ─────────────────────────────────────────────────────────────

@Service
class SecureUserService {

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) {
        System.out.println("Deleting user " + id);
    }

    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public Object getUser(Long id) {
        // Only admins OR the user themselves can access their own data
        return null;
    }

    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public Object createUser(Object request) { return null; }

    @PostAuthorize("returnObject.email == authentication.name")
    public Object getCurrentUser() {
        // Checks AFTER the method returns — verifies the returned object
        return null;
    }
}
