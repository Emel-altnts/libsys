package com.d_tech.libsys.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   DaoAuthenticationProvider authProvider,
                                                   JwtFilter jwtFilter) throws Exception {

        System.out.println("🔐 SecurityFilterChain yapılandırılıyor...");

        http
                .csrf(csrf -> {
                    System.out.println("🚫 CSRF devre dışı bırakılıyor...");
                    csrf.disable();
                })
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> {
                            System.out.println("❌ 401 Unauthorized: " + request.getRequestURI() +
                                    " - Reason: " + authException.getMessage());
                            System.out.println("📋 Request Headers:");
                            request.getHeaderNames().asIterator()
                                    .forEachRemaining(name ->
                                            System.out.println("  " + name + ": " + request.getHeader(name))
                                    );
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Yetkisiz giriş!");
                        }
                ))
                .sessionManagement(session -> {
                    System.out.println("⚙️ Session management STATELESS olarak ayarlanıyor...");
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                })
                .authorizeHttpRequests(auth -> {
                    System.out.println("🛡️ URL yetkilendirme kuralları yapılandırılıyor...");

                    auth
                            // Herkese açık endpoint'ler - Authentication gerektirmez
                            .requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/stock/**").permitAll()  // ⭐ KRITIK
                            .requestMatchers("/message", "/", "/error").permitAll()
                            .requestMatchers("/actuator/health").permitAll()

                            // Diğer tüm istekler authentication gerektirir
                            .anyRequest().authenticated();

                    System.out.println("✅ Açık endpoint'ler yapılandırıldı:");
                    System.out.println("  - /api/auth/** (tüm HTTP methodları)");
                    System.out.println("  - GET /api/books/**");
                    System.out.println("  - GET /api/stock/**  ← BU ÖNEMLİ!");
                    System.out.println("  - /message, /, /error");
                })
                .authenticationProvider(authProvider)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        System.out.println("🎯 SecurityFilterChain yapılandırması tamamlandı!");
        System.out.println("🔍 GET /api/stock/2 isteği authentication gerektirmeyecek");

        return http.build();
    }
}