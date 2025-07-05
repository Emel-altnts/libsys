package com.d_tech.libsys.security;

// JWT işlemleri için gerekli JJWT kütüphanesi sınıfları
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

// Spring Security'nin UserDetails arayüzü
import org.springframework.security.core.userdetails.UserDetails;

// Spring bileşeni olarak bu sınıfın kullanılmasını sağlar
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * JwtUtil sınıfı, JWT (JSON Web Token) oluşturmak, doğrulamak ve ayrıştırmak için yardımcı metotlar içerir.
 * Bu sınıf, uygulamadaki authentication sürecinin temel taşıdır.
 */
@Component
public class JwtUtil {

    // HMAC SHA-256 algoritması ile token'ları imzalamak için rastgele bir secret key oluşturuluyor
    // Gerçek bir projede bu anahtar sabit bir değerden (örneğin application.properties içinde) alınmalı ve gizli tutulmalı
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // Token'ın geçerlilik süresi milisaniye cinsinden (1000ms * 60s * 60dk * 24 = 24 saat)
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24;

    /**
     * Kullanıcının kullanıcı adına göre yeni bir JWT token üretir
     *
     * @param username Token'a gömülecek kullanıcı adı (subject)
     * @return İmzalanmış ve süresi belirlenmiş JWT token
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username) // Kullanıcı adı "subject" olarak belirlenir
                .setIssuedAt(new Date()) // Token'ın oluşturulma tarihi
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // Token'ın bitiş zamanı
                .signWith(SECRET_KEY) // Token, gizli anahtarla HMAC SHA-256 kullanılarak imzalanır
                .compact(); // JWT string olarak üretilir
    }

    /**
     * Token'ın geçerli olup olmadığını kontrol eder:
     * - Token'ın içinden çıkarılan kullanıcı adı ile UserDetails içindeki kullanıcı adı eşleşmeli
     * - Token süresi dolmamış olmalı
     *
     * @param token JWT token
     * @param userDetails Sistemdeki kullanıcı bilgileri
     * @return Geçerliyse true, değilse false
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Token'ın süresi dolmuş mu kontrol eder
     *
     * @param token JWT token
     * @return Süresi dolmuşsa true
     */
    private boolean isTokenExpired(String token) {
        return parseToken(token).getBody().getExpiration().before(new Date());
    }

    /**
     * Token'dan kullanıcı adını (subject) çıkartır
     *
     * @param token JWT token
     * @return Token'daki subject alanı -> kullanıcı adı
     */
    public String extractUsername(String token) {
        return parseToken(token).getBody().getSubject();
    }

    /**
     * Token'ı genel geçerlilik açısından kontrol eder:
     * - İmza doğru mu
     * - Format bozulmuş mu
     * - Süresi dolmuş mu vs.
     * Her hata durumunda log yazılır.
     *
     * @param token JWT token
     * @return Geçerliyse true, değilse false
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token); // Token ayrıştırılabiliyorsa geçerlidir
            return true;
        } catch (ExpiredJwtException e) {
            System.out.println("Token süresi dolmuş: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.out.println("Desteklenmeyen token: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.out.println("Bozuk token: " + e.getMessage());
        } catch (SignatureException e) {
            System.out.println("İmza hatası: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Token doğrulama hatası: " + e.getMessage());
        }
        return false;
    }

    /**
     * JWT token'ı ayrıştırır (decode eder), header, payload ve signature bileşenlerine ayırır
     *
     * @param token JWT token
     * @return Token'ın JWS (JSON Web Signature) yapısına ayrılmış hali
     * @throws JwtException Herhangi bir parsing hatası durumunda fırlatılır
     */
    private Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY) // Token'ı doğrulamak için kullanılan imza anahtarı
                .build()
                .parseClaimsJws(token); // Token'ı ayrıştır ve içeriğini döndür
    }
}
