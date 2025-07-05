package com.d_tech.libsys.security;

// Gerekli Servlet ve HTTP sınıfları
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Spring Security sınıfları
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

// Spring bileşeni olarak bu sınıfın kullanılmasını sağlar
import org.springframework.stereotype.Component;

// Bu filtre sadece bir kez çalışır; her istekte bir kez çağrılır
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtFilter sınıfı, her gelen HTTP isteğinde JWT (JSON Web Token) taşıyıp taşımadığını kontrol eden
 * bir güvenlik filtresidir. Eğer geçerli bir JWT varsa, ilgili kullanıcı sistemde doğrulanmış (authenticated)
 * hale getirilir. Bu filtre, Spring Security'nin filtre zincirine entegre edilmiştir.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    // JWT token'ını çözümlemek ve doğrulamak için yardımcı sınıf
    @Autowired
    private JwtUtil jwtUtil;

    // Kullanıcı bilgilerini veritabanından yüklemek için özel UserDetailsService implementasyonu
    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    /**
     * doFilterInternal metodu, her HTTP isteği geldiğinde çağrılır ve şu işlemleri yapar:
     * - Authorization header'ından JWT token'ını alır
     * - Token geçerliyse, kullanıcıyı sistemde doğrular (authenticated hale getirir)
     * - Filtre zincirinde bir sonraki adıma geçilmesini sağlar
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // "Authorization" header'ını al (örneğin: Bearer eyJhbGciOiJIUzI1...)
        String authHeader = request.getHeader("Authorization");

        String token = null;
        String username = null;

        // Authorization header'ı null değilse ve "Bearer " ile başlıyorsa:
        // - "Bearer " kısmını kesip token'ı elde et
        // - Token'dan kullanıcı adını (username) çıkart
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // "Bearer " (7 karakter) sonrası asıl token
            username = jwtUtil.extractUsername(token); // Token içinden username bilgisi alınır
        }

        // Eğer bir kullanıcı adı çıkarıldıysa ve henüz SecurityContext içinde doğrulama yoksa
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Kullanıcı bilgilerini veritabanından yükle
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Token geçerliyse (expiration süresi dolmamış, imzası doğru vs.)
            if (jwtUtil.validateToken(token, userDetails)) {

                // Kullanıcı bilgileriyle yeni bir authentication objesi oluştur
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,                 // Principal
                                null,                        // Credentials (şifre zaten token ile geldiği için null)
                                userDetails.getAuthorities() // Kullanıcının yetkileri (rol bilgisi vs.)
                        );

                // Authentication detaylarını request'e bağla (IP, session id vs.)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Authentication bilgisini SecurityContext'e kaydet -> Artık kullanıcı sistemde authenticated oldu
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // İsteği zincirdeki bir sonraki filtreye ilet (örneğin: kontrol tamamlandıktan sonra controller'a gidecek)
        filterChain.doFilter(request, response);
    }
}
