package com.d_tech.libsys.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * UserDetailsService arayüzü, Spring Security'nin kimlik doğrulama mekanizmasında kullanılan temel yapı taşlarından biridir.
 *
 * - Authentication sürecinde (örneğin login olurken) Spring Security, username'e göre kullanıcı bilgilerini bu servis aracılığıyla ister.
 * - Bu bilgileri sağlayan sınıf (örneğin: UserDetailsServiceImpl) bu arayüzü implement eder.
 * - Geri dönen bilgiler, `UserDetails` tipinde olmalıdır (kullanıcı adı, şifre, roller vs.).
 */
public interface UserDetailsService {

    /**
     * Belirtilen kullanıcı adına göre kullanıcıyı veritabanından ya da başka bir kaynaktan yükler.
     *
     * @param username Kimlik doğrulaması yapılacak kullanıcının kullanıcı adı
     * @return UserDetails nesnesi (kullanıcının şifre, rol, yetki gibi bilgilerini içerir)
     * @throws UsernameNotFoundException Kullanıcı veritabanında bulunamazsa fırlatılır
     */
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}

