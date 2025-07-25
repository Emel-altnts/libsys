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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller - kullanıcı giriş ve kayıt işlemlerini yönetir
 * Artık hem senkron hem de asenkron kayıt desteği var
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserService userService; // Senkron servis
    private final AsyncUserService asyncUserService; // Asenkron servis

    /**
     * Kullanıcı giriş endpoint'i
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            // Debug için log ekle
            System.out.println("Login attempt for username: " + request.getUsername());

            // 1. Authentication işlemi
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // 2. Kullanıcı detaylarını al
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

            // Debug için log ekle
            System.out.println("User found: " + userDetails.getUsername());
            System.out.println("User authorities: " + userDetails.getAuthorities());

            // 3. JWT token oluştur
            String token = jwtUtil.generateToken(userDetails.getUsername());

            // 4. Başarılı yanıt döndür
            return ResponseEntity.ok(new AuthResponse("Bearer " + token));

        } catch (BadCredentialsException e) {
            System.out.println("Bad credentials for username: " + request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Login failed");
        }
    }

    /**
     * Asenkron kullanıcı kayıt endpoint'i (ÖNERİLEN)
     * Kullanıcıyı bekletmez, Kafka üzerinden işlenir
     */
    @PostMapping("/signup-async")
    public ResponseEntity<SignupResponse> signupAsync(@RequestBody SignupRequest request) {
        try {
            System.out.println("Asenkron kayıt isteği alındı: username=" + request.getUsername());

            // Asenkron kayıt işlemini başlat
            SignupResponse response = asyncUserService.registerUserAsync(request);

            // Başarı durumunu kontrol et
            if (response.getMessage().contains("alındı") || response.getMessage().contains("Event ID")) {
                System.out.println("Asenkron kayıt isteği kabul edildi: username=" + request.getUsername());
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response); // 202 Accepted
            } else {
                // Validasyon hatası vs.
                System.out.println("Asenkron kayıt isteği reddedildi: username=" + request.getUsername() +
                        " - " + response.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            System.out.println("Asenkron kayıt hatası: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new SignupResponse("Kayıt isteği alınamadı. Lütfen tekrar deneyin."));
        }
    }

    /**
     * Senkron kullanıcı kayıt endpoint'i (GERİYE UYUMLULUK İÇİN)
     * Eski sistemlerle uyumlu olmak için tutuldu
     */
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@RequestBody SignupRequest request) {
        try {
            System.out.println("Senkron kayıt isteği alındı: username=" + request.getUsername());

            // Senkron kayıt işlemini gerçekleştir
            SignupResponse response = userService.registerUser(request);

            // Başarı durumunu kontrol et
            if (response.getMessage().contains("başarıyla")) {
                System.out.println("Senkron kayıt başarılı: username=" + request.getUsername());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                // Kayıt başarısız (validasyon hatası vs.)
                System.out.println("Senkron kayıt başarısız: username=" + request.getUsername() +
                        " - " + response.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            System.out.println("Senkron kayıt hatası: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new SignupResponse("Kayıt işlemi sırasında beklenmeyen bir hata oluştu!"));
        }
    }

    /**
     * Kayıt durumu sorgulama endpoint'i
     * Asenkron kayıt sonrası kullanıcı durumunu öğrenmek için
     */
    @GetMapping("/registration-status/{eventId}")
    public ResponseEntity<String> getRegistrationStatus(@PathVariable String eventId) {
        try {
            System.out.println("Kayıt durumu sorgulanıyor: eventId=" + eventId);

            String status = asyncUserService.getRegistrationStatus(eventId);
            return ResponseEntity.ok(status);

        } catch (Exception e) {
            System.out.println("Kayıt durumu sorgulama hatası: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Durum sorgulanamadı");
        }
    }

    /**
     * Kullanıcı adı kontrol endpoint'i (opsiyonel - frontend için kullanışlı)
     */
    @GetMapping("/check-username/{username}")
    public ResponseEntity<Boolean> checkUsername(@PathVariable String username) {
        boolean exists = userService.existsByUsername(username);
        return ResponseEntity.ok(exists);
    }

    /**
     * Sistem durumu endpoint'i
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running");
    }
}