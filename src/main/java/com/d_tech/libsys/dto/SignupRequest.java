package com.d_tech.libsys.dto;

import lombok.Data;

/**
 * SignupRequest sınıfı, kullanıcı kayıt işlemi sırasında istemciden (client) gelen verileri tutar.
 * Bu sınıf bir DTO'dur (Data Transfer Object) ve genellikle HTTP POST isteğiyle birlikte JSON formatında gönderilir.
 *
 * Örnek JSON:
 * {
 *   "username": "newuser",
 *   "password": "password123",
 *   "confirmPassword": "password123"
 * }
 */
@Data // Lombok: Getter, Setter, toString, equals, hashCode otomatik eklenir
public class SignupRequest {

    // Kullanıcının kaydolmak için gönderdiği kullanıcı adı
    private String username;

    // Kullanıcının kaydolmak için gönderdiği şifre
    private String password;

    // Şifre onayı - güvenlik için şifrenin doğru girildiğinden emin olmak için
    private String confirmPassword;
}
