package com.d_tech.libsys.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 24 hours

    public String generateToken(String username) {
        System.out.println("🎫 JWT Token oluşturuluyor: username=" + username);

        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();

        System.out.println("✅ JWT Token oluşturuldu:");
        System.out.println("   ├── Username: " + username);
        System.out.println("   ├── Token uzunluğu: " + token.length());
        System.out.println("   ├── İlk 30 karakter: " + token.substring(0, Math.min(token.length(), 30)) + "...");
        System.out.println("   └── Expiration: " + new Date(System.currentTimeMillis() + EXPIRATION_TIME));

        return token;
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        System.out.println("🔐 JWT Token validation başlatılıyor:");
        System.out.println("   ├── UserDetails username: " + userDetails.getUsername());
        System.out.println("   └── Token uzunluğu: " + token.length());

        try {
            final String username = extractUsername(token);
            System.out.println("   ├── Token'dan çıkarılan username: " + username);

            boolean usernameMatches = username.equals(userDetails.getUsername());
            System.out.println("   ├── Username eşleşmesi: " + usernameMatches);

            boolean tokenExpired = isTokenExpired(token);
            System.out.println("   ├── Token süresi dolmuş mu: " + tokenExpired);

            boolean valid = usernameMatches && !tokenExpired;
            System.out.println("   └── Final validation result: " + valid);

            return valid;
        } catch (Exception e) {
            System.out.println("❌ Token validation hatası:");
            System.out.println("   ├── Exception type: " + e.getClass().getSimpleName());
            System.out.println("   └── Message: " + e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        try {
            Date expiration = parseToken(token).getBody().getExpiration();
            Date now = new Date();
            boolean expired = expiration.before(now);

            System.out.println("🕐 Token expiration kontrolü:");
            System.out.println("   ├── Token expiration: " + expiration);
            System.out.println("   ├── Şu anki zaman: " + now);
            System.out.println("   └── Süresi dolmuş mu: " + expired);

            return expired;
        } catch (Exception e) {
            System.out.println("❌ Token expiration kontrol hatası: " + e.getMessage());
            return true; // Hata varsa expired olarak kabul et
        }
    }

    public String extractUsername(String token) {
        try {
            String username = parseToken(token).getBody().getSubject();
            System.out.println("👤 Username çıkarıldı: " + username);
            return username;
        } catch (ExpiredJwtException e) {
            System.out.println("❌ Token süresi dolmuş: " + e.getMessage());
            System.out.println("   └── Süresi dolan username: " + e.getClaims().getSubject());
            throw e;
        } catch (Exception e) {
            System.out.println("❌ Username çıkarma hatası:");
            System.out.println("   ├── Exception type: " + e.getClass().getSimpleName());
            System.out.println("   └── Message: " + e.getMessage());
            throw e;
        }
    }

    public boolean validateToken(String token) {
        System.out.println("🔐 Simple token validation:");
        try {
            parseToken(token);
            System.out.println("✅ Token format geçerli");
            return true;
        } catch (ExpiredJwtException e) {
            System.out.println("❌ Token süresi dolmuş: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.out.println("❌ Desteklenmeyen token: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.out.println("❌ Bozuk token format: " + e.getMessage());
        } catch (SecurityException e) {
            System.out.println("❌ Token güvenlik hatası: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Token argument hatası: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Genel token doğrulama hatası: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private Jws<Claims> parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token);
        } catch (Exception e) {
            System.out.println("❌ Token parse hatası:");
            System.out.println("   ├── Exception type: " + e.getClass().getSimpleName());
            System.out.println("   └── Message: " + e.getMessage());
            throw e;
        }
    }
}