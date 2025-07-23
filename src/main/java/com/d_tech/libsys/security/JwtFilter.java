package com.d_tech.libsys.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        System.out.println("\n🔍 ===========================================");
        System.out.println("🔍 JWT Filter - İşlenen istek: " + method + " " + requestUri);
        System.out.println("🔍 ===========================================");

        // Public endpoint'ler için JWT kontrolü yapmadan geç
        if (isPublicEndpoint(method, requestUri)) {
            System.out.println("✅ Public endpoint - JWT kontrolü atlanıyor: " + requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        System.out.println("🔐 Authorization Header: " + authHeader);
        System.out.println("🔐 Header null mu? " + (authHeader == null));
        System.out.println("🔐 Header boş mu? " + (authHeader != null && authHeader.isEmpty()));

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            System.out.println("🎟️ Token alındı:");
            System.out.println("   ├── Uzunluk: " + token.length());
            System.out.println("   ├── İlk 20 karakter: " + token.substring(0, Math.min(token.length(), 20)) + "...");
            System.out.println("   └── Son 10 karakter: ..." + token.substring(Math.max(0, token.length() - 10)));

            try {
                username = jwtUtil.extractUsername(token);
                System.out.println("👤 Token'dan çıkarılan username: " + username);
            } catch (Exception e) {
                System.out.println("❌ Token parse hatası:");
                System.out.println("   ├── Exception type: " + e.getClass().getSimpleName());
                System.out.println("   ├── Message: " + e.getMessage());
                System.out.println("   └── Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "null"));
            }
        } else {
            System.out.println("⚠️ Bearer token bulunamadı!");
            if (authHeader == null) {
                System.out.println("   └── Authorization header hiç yok");
            } else if (!authHeader.startsWith("Bearer ")) {
                System.out.println("   └── Authorization header 'Bearer ' ile başlamıyor: " + authHeader);
            }
        }

        // Mevcut authentication kontrol et
        System.out.println("🔍 Mevcut SecurityContext Authentication: " +
                (SecurityContextHolder.getContext().getAuthentication() != null ?
                        SecurityContextHolder.getContext().getAuthentication().getName() : "null"));

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println("🔍 User details yükleniyor: " + username);

            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                System.out.println("✅ User details yüklendi:");
                System.out.println("   ├── Username: " + userDetails.getUsername());
                System.out.println("   ├── Enabled: " + userDetails.isEnabled());
                System.out.println("   ├── Account non-expired: " + userDetails.isAccountNonExpired());
                System.out.println("   ├── Account non-locked: " + userDetails.isAccountNonLocked());
                System.out.println("   ├── Credentials non-expired: " + userDetails.isCredentialsNonExpired());
                System.out.println("   └── Authorities: " + userDetails.getAuthorities());

                boolean tokenValid = jwtUtil.validateToken(token, userDetails);
                System.out.println("🔐 Token validation result: " + tokenValid);

                if (tokenValid) {
                    System.out.println("✅ JWT token geçerli - Authentication oluşturuluyor");

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    System.out.println("✅ Authentication context'e set edildi:");
                    System.out.println("   ├── Principal: " + authToken.getPrincipal());
                    System.out.println("   └── Authorities: " + authToken.getAuthorities());
                } else {
                    System.out.println("❌ JWT token geçersiz - Authentication set edilmedi");
                }
            } catch (Exception e) {
                System.out.println("❌ User details yükleme hatası:");
                System.out.println("   ├── Exception type: " + e.getClass().getSimpleName());
                System.out.println("   ├── Message: " + e.getMessage());
                System.out.println("   └── Stack trace: ");
                e.printStackTrace();
            }
        } else if (username == null && authHeader != null) {
            System.out.println("⚠️ Token var ama username çıkarılamadı");
        } else if (username != null && SecurityContextHolder.getContext().getAuthentication() != null) {
            System.out.println("ℹ️ Username var ve zaten authentication set edilmiş");
        }

        System.out.println("🔍 Filter tamamlandı - Next filter'a geçiliyor...");
        System.out.println("🔍 ===========================================\n");

        filterChain.doFilter(request, response);
    }

    /**
     * Public endpoint kontrolü - SADECE AUTH endpoint'leri public
     */
    private boolean isPublicEndpoint(String method, String uri) {
        // ✅ Auth endpoint'leri
        if (uri.startsWith("/api/auth/")) {
            System.out.println("✅ Public endpoint tespit edildi: AUTH endpoint");
            return true;
        }

        // ✅ Message endpoint
        if (uri.equals("/message")) {
            System.out.println("✅ Public endpoint tespit edildi: Message endpoint");
            return true;
        }

        // ✅ Error endpoint
        if (uri.equals("/error")) {
            System.out.println("✅ Public endpoint tespit edildi: Error endpoint");
            return true;
        }

        System.out.println("🔒 Protected endpoint tespit edildi - Authentication gerekli");
        return false;
    }
}