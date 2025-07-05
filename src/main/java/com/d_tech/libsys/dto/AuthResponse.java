package com.d_tech.libsys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * AuthResponse sınıfı, kullanıcı başarılı bir şekilde giriş yaptıktan sonra
 * istemciye (frontend'e) döndürülen JWT (JSON Web Token) bilgisini içerir.
 *
 * Bu sınıf, AuthController tarafından kullanılır ve yanıt (response) gövdesi olarak gönderilir.
 *
 * Örnek Response JSON:
 * {
 *   "token": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
 * }
 */
@Data // Lombok: Getter, Setter, toString, equals, hashCode otomatik tanımlar
@AllArgsConstructor // Lombok: Parametreli constructor otomatik oluşturur (AuthResponse(String token))
public class AuthResponse {

    // Kullanıcıya döndürülecek JWT (token), genellikle "Bearer <token>" formatında olur
    private String token;
}
