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

        // GET /api/stock/** istekleri için özel kontrol
        if ("GET".equals(method) && requestUri.startsWith("/api/stock/")) {
            System.out.println("✅ GET /api/stock/** isteği - JWT kontrolü atlanıyor (permitAll)");
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        System.out.println("🔐 Authorization Header: " +
                (authHeader != null ? authHeader.substring(0, Math.min(authHeader.length(), 20)) + "..." : "YOK"));

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(token);
                System.out.println("👤 Token'dan çıkarılan username: " + username);
            } catch (Exception e) {
                System.out.println("❌ Token parse hatası: " + e.getMessage());
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
        } else if (username == null) {
            System.out.println("⚠️ Token'dan username çıkarılamadı");
        } else {
            System.out.println("ℹ️ Zaten authentication mevcut");
        }

        filterChain.doFilter(request, response);
    }
}