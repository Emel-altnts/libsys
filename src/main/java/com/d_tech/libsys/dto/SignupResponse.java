package com.d_tech.libsys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * SignupResponse sınıfı, kullanıcı başarılı bir şekilde kaydolduktan sonra
 * istemciye döndürülen mesaj bilgisini içerir.
 *
 * Bu sınıf, AuthController tarafından kullanılır ve yanıt (response) gövdesi olarak gönderilir.
 *
 * Örnek Response JSON:
 * {
 *   "message": "Kullanıcı başarıyla kaydedildi"
 * }
 */
@Data // Lombok: Getter, Setter, toString, equals, hashCode otomatik tanımlar
@AllArgsConstructor // Lombok: Parametreli constructor otomatik oluşturur (SignupResponse(String message))
public class SignupResponse {

    // Kullanıcıya döndürülecek başarı veya hata mesajı
    private String message;
}