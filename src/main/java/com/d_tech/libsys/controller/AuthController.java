package com.d_tech.libsys.controller;

import com.d_tech.libsys.dto.AuthRequest;
import com.d_tech.libsys.dto.AuthResponse;
import com.d_tech.libsys.dto.SignupRequest;
import com.d_tech.libsys.dto.SignupResponse;
import com.d_tech.libsys.security.JwtUtil;
import com.d_tech.libsys.security.UserDetailsServiceImpl;
import com.d_tech.libsys.service.AsyncUserService;
import com.d_tech.libsys.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller - Railway Optimized
 * Handles both sync and async user registration based on Kafka availability
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserService userService;
    private final AsyncUserService asyncUserService;

    @Value("${app.kafka.enabled:false}")
    private boolean kafkaEnabled;

    /**
     * User login endpoint
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            log.info("Login attempt for username: {}", request.getUsername());

            // Authentication
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            log.info("User found: {}, authorities: {}", userDetails.getUsername(), userDetails.getAuthorities());

            // Generate JWT token
            String token = jwtUtil.generateToken(userDetails.getUsername());

            return ResponseEntity.ok(new AuthResponse("Bearer " + token));

        } catch (BadCredentialsException e) {
            log.warn("Bad credentials for username: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Invalid credentials"));
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Login failed"));
        }
    }

    /**
     * User registration endpoint - chooses sync or async based on Kafka availability
     */
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@RequestBody SignupRequest request) {
        log.info("Registration request: username={}, kafka={}", request.getUsername(), kafkaEnabled);

        try {
            SignupResponse response;
            HttpStatus status;

            if (kafkaEnabled) {
                // Async registration with Kafka
                response = asyncUserService.registerUserAsync(request);
                status = response.getMessage().contains("alındı") || response.getMessage().contains("Event ID")
                        ? HttpStatus.ACCEPTED : HttpStatus.BAD_REQUEST;
            } else {
                // Sync registration without Kafka
                response = userService.registerUser(request);
                status = response.getMessage().contains("başarıyla")
                        ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
            }

            log.info("Registration response: username={}, success={}",
                    request.getUsername(), status.is2xxSuccessful());

            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            log.error("Registration error: username={}, error={}", request.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new SignupResponse("Kayıt işlemi sırasında beklenmeyen bir hata oluştu!"));
        }
    }

    /**
     * Legacy async signup endpoint (for backward compatibility)
     */
    @PostMapping("/signup-async")
    public ResponseEntity<SignupResponse> signupAsync(@RequestBody SignupRequest request) {
        if (!kafkaEnabled) {
            log.warn("Async signup requested but Kafka is disabled, falling back to sync");
            return signup(request);
        }

        return signup(request); // Uses async internally when Kafka is enabled
    }

    /**
     * Registration status check (only works with Kafka)
     */
    @GetMapping("/registration-status/{eventId}")
    public ResponseEntity<String> getRegistrationStatus(@PathVariable String eventId) {
        if (!kafkaEnabled) {
            return ResponseEntity.ok("Kafka disabled - check user directly in database");
        }

        try {
            log.info("Registration status query: eventId={}", eventId);
            String status = asyncUserService.getRegistrationStatus(eventId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Registration status query error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Status could not be retrieved");
        }
    }

    /**
     * Username availability check
     */
    @GetMapping("/check-username/{username}")
    public ResponseEntity<UsernameCheckResponse> checkUsername(@PathVariable String username) {
        boolean exists = userService.existsByUsername(username);
        return ResponseEntity.ok(new UsernameCheckResponse(exists, exists ? "Username taken" : "Username available"));
    }

    /**
     * System health check
     */
    @GetMapping("/health")
    public ResponseEntity<SystemStatusResponse> health() {
        return ResponseEntity.ok(SystemStatusResponse.builder()
                .status("UP")
                .kafkaEnabled(kafkaEnabled)
                .registrationMode(kafkaEnabled ? "ASYNC" : "SYNC")
                .timestamp(System.currentTimeMillis())
                .build());
    }

    /**
     * API info endpoint
     */
    @GetMapping("/info")
    public ResponseEntity<ApiInfoResponse> info() {
        return ResponseEntity.ok(ApiInfoResponse.builder()
                .applicationName("LibSys Library Management System")
                .version("1.0.0")
                .kafkaEnabled(kafkaEnabled)
                .registrationMode(kafkaEnabled ? "ASYNC" : "SYNC")
                .endpoints(java.util.List.of(
                        "POST /api/auth/login - User login",
                        "POST /api/auth/signup - User registration",
                        "GET /api/auth/check-username/{username} - Check username availability",
                        "GET /api/books - List all books",
                        "GET /api/users - List users (Admin only)"
                ))
                .build());
    }

    // Helper methods and DTOs
    private ErrorResponse createErrorResponse(String message) {
        return new ErrorResponse(message, System.currentTimeMillis());
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String error;
        private long timestamp;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class UsernameCheckResponse {
        private boolean exists;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SystemStatusResponse {
        private String status;
        private boolean kafkaEnabled;
        private String registrationMode;
        private long timestamp;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ApiInfoResponse {
        private String applicationName;
        private String version;
        private boolean kafkaEnabled;
        private String registrationMode;
        private java.util.List<String> endpoints;
    }
}