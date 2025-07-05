package com.d_tech.libsys.dto;

import lombok.Data;

/**
 * AuthRequest sınıfı, login işlemi sırasında istemciden (client) gelen verileri tutar.
 * Bu sınıf bir DTO'dur (Data Transfer Object) ve genellikle HTTP POST isteğiyle birlikte JSON formatında gönderilir.
 *
 * Örnek JSON:
 * {
 *   "username": "testuser",
 *   "password": "secret123"
 * }
 */
@Data // Lombok: Getter, Setter, toString, equals, hashCode otomatik eklenir
public class AuthRequest {

    // Kullanıcının giriş yapmak için gönderdiği kullanıcı adı
    private String username;

    // Kullanıcının giriş yapmak için gönderdiği şifre
    private String password;
}

