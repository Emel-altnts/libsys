package com.d_tech.libsys.service;

import com.d_tech.libsys.dto.SignupRequest;
import com.d_tech.libsys.dto.SignupResponse;

/**
 * UserService interface - kullanıcı işlemleri için servis katmanı arayüzü
 */
public interface UserService {

    /**
     * Yeni kullanıcı kayıt işlemi
     * @param signupRequest Kayıt bilgileri
     * @return Kayıt sonucu mesajı
     */
    SignupResponse registerUser(SignupRequest signupRequest);

    /**
     * Kullanıcı adının mevcut olup olmadığını kontrol eder
     * @param username Kontrol edilecek kullanıcı adı
     * @return Varsa true, yoksa false
     */
    boolean existsByUsername(String username);
}