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
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> {
                            System.out.println("❌ 401 Unauthorized: " + request.getRequestURI());
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Yetkisiz giriş!");
                        }
                ))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    System.out.println("🛡️ URL yetkilendirme kuralları yapılandırılıyor...");

                    auth
                            // Herkese açık endpoint'ler
                            .requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/stock/**").permitAll()

                            // ✅ STOK ENDPOINT'LERİNİ HERKESE AÇ (TEST İÇİN)
                            .requestMatchers(HttpMethod.POST, "/api/stock/**").permitAll()
                            .requestMatchers(HttpMethod.PUT, "/api/stock/**").permitAll()
                            .requestMatchers(HttpMethod.DELETE, "/api/stock/**").permitAll()

                            .requestMatchers("/message").permitAll()

                            // Diğer tüm istekler authentication gerektirir
                            .anyRequest().authenticated();

                    System.out.println("✅ Stok endpoint'leri herkese açık olarak ayarlandı");
                })
                .authenticationProvider(authProvider)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        System.out.println("🎯 SecurityFilterChain yapılandırması tamamlandı!");
        return http.build();
    }
}