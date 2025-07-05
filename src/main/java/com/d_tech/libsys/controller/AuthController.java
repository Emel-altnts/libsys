package com.d_tech.libsys.controller;

import com.d_tech.libsys.dto.AuthRequest;          // Giriş isteği DTO (username, password içerir)
import com.d_tech.libsys.dto.AuthResponse;         // Giriş yanıtı DTO (JWT token içerir)
import com.d_tech.libsys.security.JwtUtil;         // Token üretimi ve doğrulaması için yardımcı sınıf
import com.d_tech.libsys.security.UserDetailsServiceImpl; // Kullanıcıyı veritabanından getiren servis

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Bu controller, /api/auth/login endpoint'ini yönetir.
 * Kullanıcıdan username ve password alır, doğrulama başarılı olursa JWT token döner.
 */
@RestController
@RequestMapping("/api/auth") // Bu controller altındaki tüm endpoint'ler /api/auth ile başlar
@RequiredArgsConstructor     // final alanlar için constructor otomatik olarak oluşturulur (Lombok)
public class AuthController {

    // Kimlik doğrulama işlemini yapan servis (Spring Security tarafından sağlanır)
    private final AuthenticationManager authenticationManager;

    // JWT token üretimi ve kontrolü yapan yardımcı sınıf
    private final JwtUtil jwtUtil;

    // Kullanıcı bilgilerini (username, şifre, roller) veritabanından yükleyen servis
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Kullanıcı giriş işlemini gerçekleştirir.
     * - Username ve password ile authentication yapılır.
     * - Başarılıysa kullanıcıya JWT token döndürülür.
     * - Başarısızsa HTTP 401 hatası verilir.
     *
     * @param request Giriş bilgilerini (username, password) içeren DTO
     * @return JWT token veya hata mesajı
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            // 1. Kullanıcı adı ve şifre ile authentication işlemi yapılır
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // 2. Authentication başarılı ise kullanıcı detayları alınır
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

            // 3. Kullanıcı adı ile yeni bir JWT token üretilir
            String token = jwtUtil.generateToken(userDetails.getUsername());

            // 4. Başarılı girişte token, "Bearer <token>" formatında döndürülür
            return ResponseEntity.ok(new AuthResponse("Bearer " + token));
        } catch (BadCredentialsException e) {
            // 5. Hatalı kullanıcı adı ya da şifre durumunda 401 hatası döndürülür
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }
}
