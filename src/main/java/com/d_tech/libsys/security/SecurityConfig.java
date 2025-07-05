package com.d_tech.libsys.security;

// Özel JWT filtremizi projeye dahil ediyoruz
import com.d_tech.libsys.security.JwtFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Bu sınıf, uygulamanın güvenlik yapılandırmalarını içerir.
 * - Hangi endpoint'lerin korunduğu,
 * - Hangi filtrelerin çalıştığı,
 * - Oturum yönetiminin nasıl yapıldığı gibi ayarları burada tanımlarız.
 */
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity // @PreAuthorize, @Secured gibi anotasyonların aktif olmasını sağlar
public class SecurityConfig {

    // JWT doğrulamasını yapan özel filtre
    private final JwtFilter jwtFilter;

    // Kullanıcı bilgilerini sağlayan servis; authentication işlemlerinde kullanılır
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * Spring Security'nin filtre zincirini tanımlar.
     * Burada:
     * - CSRF'nin kapatılması,
     * - Hangi URL'lerin public olduğu,
     * - JWT filtresinin eklenmesi gibi işlemler yapılır.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF (Cross Site Request Forgery) korunması devre dışı bırakılıyor.
                // Çünkü JWT kullanıldığında sunucuya oturum (session) tutulmaz, dolayısıyla CSRF riski yoktur.
                .csrf(csrf -> csrf.disable())

                // Yetkisiz erişimlerde verilecek yanıt burada tanımlanıyor
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                (request, response, authException) ->
                                        // 401 Unauthorized cevabı döner
                                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Yetkisiz giriş!")
                        )
                )

                // Oturum (session) yönetimi ayarlanıyor:
                // STATELESS = Her istek bağımsızdır, sunucu tarafında oturum tutulmaz (her istekte JWT gerekir)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Hangi isteklerin serbest olduğu, hangilerinin kimlik doğrulaması gerektirdiği belirleniyor
                .authorizeHttpRequests(auth -> auth
                        // /api/auth/** altındaki tüm endpoint'ler (login, register vs.) açık
                        .requestMatchers("/api/auth/**").permitAll()

                        // GET istekleriyle kitapları listeleme işlemi herkes için serbest
                        .requestMatchers(HttpMethod.GET, "/api/books/**").permitAll()

                        // Diğer tüm istekler (örneğin kitap ekleme, silme) kimlik doğrulaması ister
                        .anyRequest().authenticated()
                );

        // JWT filtresi, Spring Security'nin kendi authentication filtresinden önce çalışmalı
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        // Tanımlanan güvenlik zinciri uygulanmak üzere geri döndürülüyor
        return http.build();
    }

    /**
     * AuthenticationManager bean'i, kimlik doğrulama işlemlerinde kullanılır.
     * Örneğin bir login servisinde kullanıcı adı/parola kontrolü yapılırken devreye girer.
     *
     * @param config Spring Security'nin otomatik konfigürasyon nesnesi
     * @return AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
