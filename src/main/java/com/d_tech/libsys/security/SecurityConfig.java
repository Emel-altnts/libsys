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
@EnableMethodSecurity(prePostEnabled = true)
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
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> {
                            System.out.println("🚨 AUTHENTICATION ENTRY POINT TRIGGERED!");
                            System.out.println("❌ 401 Unauthorized: " + request.getMethod() + " " + request.getRequestURI());
                            System.out.println("🔍 Auth Header: " + request.getHeader("Authorization"));
                            System.out.println("💥 Exception: " + authException.getMessage());

                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\n" +
                                    "  \"error\": \"Unauthorized\",\n" +
                                    "  \"message\": \"" + authException.getMessage() + "\",\n" +
                                    "  \"path\": \"" + request.getRequestURI() + "\",\n" +
                                    "  \"method\": \"" + request.getMethod() + "\",\n" +
                                    "  \"timestamp\": \"" + java.time.Instant.now() + "\"\n" +
                                    "}");
                        }
                ))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    System.out.println("🛡️ URL yetkilendirme kuralları yapılandırılıyor...");

                    auth
                            // ✅ Herkese açık endpoint'ler - NO AUTH REQUIRED
                            .requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers("/message").permitAll()
                            .requestMatchers("/error").permitAll()
                            .requestMatchers("/h2-console/**").permitAll()

                            // 🔓 TEMPORARY: Test için tüm endpoint'leri authenticated yap (ADMIN role check yok)
                            .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                            .requestMatchers(HttpMethod.POST, "/api/**").authenticated()
                            .requestMatchers(HttpMethod.PUT, "/api/**").authenticated()
                            .requestMatchers(HttpMethod.DELETE, "/api/**").authenticated()

                            // ✅ Diğer tüm istekler authentication gerektirir
                            .anyRequest().authenticated();

                    System.out.println("✅ URL yetkilendirme kuralları tamamlandı - TÜM API ENDPOINT'LER AUTHENTICATED");
                })
                .authenticationProvider(authProvider)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        System.out.println("🎯 SecurityFilterChain tamamlandı!");
        return http.build();
    }
}