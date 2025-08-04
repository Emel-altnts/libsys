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
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * ✅ ENHANCED: Security Configuration with CORS support for Ngrok
 */
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

    /**
     * ✅ CRITICAL: CORS Configuration for Ngrok and Frontend
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        System.out.println("🌐 CORS Configuration oluşturuluyor - Ngrok için optimize edildi");

        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ Allow all origins for ngrok compatibility
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "https://*.ngrok-free.app",
                "https://*.ngrok.io",
                "https://*.ngrok.com",
                "http://localhost:*",
                "https://localhost:*",
                "http://127.0.0.1:*",
                "https://127.0.0.1:*",
                "*"  // For development only
        ));

        // ✅ Allow all common HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));

        // ✅ Allow all headers (especially important for JWT)
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // ✅ Allow credentials (for JWT cookies if needed)
        configuration.setAllowCredentials(true);

        // ✅ Set max age for preflight requests
        configuration.setMaxAge(3600L);

        // ✅ Expose common headers to frontend
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-Total-Count",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials",
                "Content-Type"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        System.out.println("✅ CORS yapılandırması tamamlandı - Tüm origins ve methods destekleniyor");
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   DaoAuthenticationProvider authProvider,
                                                   JwtFilter jwtFilter,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {

        System.out.println("🔐 SecurityFilterChain yapılandırılıyor - Ngrok Edition...");

        http
                // ✅ CORS configuration - MUST be first
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // ✅ CSRF disabled for API usage
                .csrf(AbstractHttpConfigurer::disable)

                // ✅ Exception handling with CORS headers
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> {
                            System.out.println("🚨 AUTHENTICATION ENTRY POINT TRIGGERED!");
                            System.out.println("❌ 401 Unauthorized: " + request.getMethod() + " " + request.getRequestURI());
                            System.out.println("🔍 Origin: " + request.getHeader("Origin"));
                            System.out.println("🔍 Auth Header: " + request.getHeader("Authorization"));
                            System.out.println("💥 Exception: " + authException.getMessage());

                            // ✅ Set CORS headers for error responses
                            response.setHeader("Access-Control-Allow-Origin", "*");
                            response.setHeader("Access-Control-Allow-Credentials", "true");
                            response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
                            response.setHeader("Access-Control-Allow-Headers", "*");

                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\n" +
                                    "  \"error\": \"Unauthorized\",\n" +
                                    "  \"message\": \"" + authException.getMessage() + "\",\n" +
                                    "  \"path\": \"" + request.getRequestURI() + "\",\n" +
                                    "  \"method\": \"" + request.getMethod() + "\",\n" +
                                    "  \"timestamp\": \"" + java.time.Instant.now() + "\",\n" +
                                    "  \"hint\": \"Include 'Authorization: Bearer <token>' header\"\n" +
                                    "}");
                        }
                ))

                // ✅ Session management
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ✅ Authorization rules - Ngrok friendly
                .authorizeHttpRequests(auth -> {
                    System.out.println("🛡️ URL yetkilendirme kuralları yapılandırılıyor - Ngrok Edition...");

                    auth
                            // ✅ Public endpoints - NO AUTH REQUIRED
                            .requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers("/message").permitAll()
                            .requestMatchers("/error").permitAll()

                            // ✅ Health and monitoring endpoints - PUBLIC for ngrok demo
                            .requestMatchers("/actuator/**").permitAll()
                            .requestMatchers("/h2-console/**").permitAll()

                            // ✅ CORS preflight requests
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                            // ✅ API endpoints - AUTHENTICATED
                            .requestMatchers(HttpMethod.GET, "/api/books/**").authenticated()
                            .requestMatchers(HttpMethod.GET, "/api/stock/**").authenticated()
                            .requestMatchers(HttpMethod.GET, "/api/users/**").hasRole("ADMIN")

                            // ✅ Write operations - AUTHENTICATED
                            .requestMatchers(HttpMethod.POST, "/api/**").authenticated()
                            .requestMatchers(HttpMethod.PUT, "/api/**").authenticated()
                            .requestMatchers(HttpMethod.DELETE, "/api/**").authenticated()

                            // ✅ Admin endpoints
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")
                            .requestMatchers("/api/stock/orders/**").hasRole("ADMIN")
                            .requestMatchers("/api/invoices/**").hasRole("ADMIN")

                            // ✅ All other requests require authentication
                            .anyRequest().authenticated();

                    System.out.println("✅ URL yetkilendirme kuralları tamamlandı - Ngrok Ready");
                })

                // ✅ H2 Console support (for ngrok demo)
                .headers(headers -> headers.frameOptions().disable())

                // ✅ Authentication provider
                .authenticationProvider(authProvider)

                // ✅ JWT filter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        System.out.println("🎯 SecurityFilterChain tamamlandı - Ngrok Edition!");
        System.out.println("🌐 CORS: Enabled for all origins");
        System.out.println("🔐 JWT: Enabled");
        System.out.println("🏥 H2 Console: Enabled at /h2-console");
        System.out.println("📊 Actuator: Public access enabled");

        return http.build();
    }
}