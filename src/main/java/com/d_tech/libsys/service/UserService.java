package com.d_tech.libsys.service;

import com.d_tech.libsys.dto.SignupRequest;
import com.d_tech.libsys.dto.SignupResponse;

/**
 * UserService interface - kullanıcı işlemleri için servis katmanı arayüzü
 */
public interface UserService {

    /**
     * Yeni kullanıcı kayıt işlemi
     */
    SignupResponse registerUser(SignupRequest signupRequest);

    /**
     * Kullanıcı adının mevcut olup olmadığını kontrol eder
     */
    boolean existsByUsername(String username);
}