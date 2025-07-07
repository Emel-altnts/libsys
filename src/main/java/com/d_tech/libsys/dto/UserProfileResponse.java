package com.d_tech.libsys.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Kullanıcı profil bilgilerini döndürmek için kullanılan DTO.
 * - Kullanıcı adı ve rol bilgisi içerir.
 */
@Data
@AllArgsConstructor
public class UserProfileResponse {
    private String username;
    private String role;
}
