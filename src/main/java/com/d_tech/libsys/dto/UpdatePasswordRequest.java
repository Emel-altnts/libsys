package com.d_tech.libsys.dto;

import lombok.Data;

/**
 * Kullanıcının şifresini güncellemesi için gönderdiği DTO.
 * - Mevcut şifresi ve yeni şifresi alanlarını içerir.
 */
@Data
public class UpdatePasswordRequest {
    private String oldPassword;
    private String newPassword;
}

