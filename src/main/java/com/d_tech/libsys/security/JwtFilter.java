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

        System.out.println("🔍 JWT Filter - İşlenen istek: " + method + " " + requestUri);

        // Public endpoint'ler için JWT kontrolü yapmadan geç
        if (isPublicEndpoint(method, requestUri)) {
            System.out.println("✅ Public endpoint - JWT kontrolü atlanıyor: " + requestUri);
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        System.out.println("🔐 Authorization Header: " +
                (authHeader != null ? "Bearer " + authHeader.substring(7, Math.min(authHeader.length(), 20)) + "..." : "YOK"));

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            System.out.println("🎟️ Token alındı, uzunluk: " + token.length());

            try {
                username = jwtUtil.extractUsername(token);
                System.out.println("👤 Token'dan çıkarılan username: " + username);
            } catch (Exception e) {
                System.out.println("❌ Token parse hatası: " + e.getMessage());
                // Token parse hatası - devam et, Spring Security handle edecek
            }
        } else {
            System.out.println("⚠️ Bearer token bulunamadı");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            System.out.println("🔍 User details yükleniyor: " + username);

            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                System.out.println("✅ User details yüklendi: " + userDetails.getUsername());
                System.out.println("🔑 User authorities: " + userDetails.getAuthorities());

                if (jwtUtil.validateToken(token, userDetails)) {
                    System.out.println("✅ JWT token geçerli");

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    System.out.println("✅ Authentication context'e set edildi");
                } else {
                    System.out.println("❌ JWT token geçersiz");
                }
            } catch (Exception e) {
                System.out.println("❌ User details yükleme hatası: " + e.getMessage());
            }
        } else if (username == null && authHeader != null) {
            System.out.println("⚠️ Token var ama username çıkarılamadı");
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Public endpoint kontrolü
     */
    private boolean isPublicEndpoint(String method, String uri) {
        // Auth endpoint'leri
        if (uri.startsWith("/api/auth/")) {
            return true;
        }

        // Message endpoint
        if (uri.equals("/message")) {
            return true;
        }

        // GET requests for read-only operations
        if ("GET".equals(method)) {
            return uri.startsWith("/api/books/") ||
                    uri.startsWith("/api/stock/") ||
                    uri.startsWith("/api/invoices/");
        }

        return false;
    }
}