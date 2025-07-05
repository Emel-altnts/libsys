package com.d_tech.libsys.security;

import com.d_tech.libsys.domain.model.User;         // Kendi uygulamamızdaki User entity
import com.d_tech.libsys.repository.UserRepository; // User'ı veritabanından getiren repository

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * UserDetailsServiceImpl sınıfı, Spring Security'nin kullanıcıyı kimlik doğrulama (authentication) sürecinde
 * veritabanından bulması için gerekli olan UserDetailsService arayüzünün implementasyonudur.
 *
 * Bu sınıf, bir kullanıcı adı alır ve ilgili kullanıcıyı veritabanında arar.
 * Eğer bulursa, bu kullanıcıyı Spring Security'nin anlayabileceği `UserDetails` nesnesine dönüştürür.
 */
@Service
public class UserDetailsServiceImpl implements org.springframework.security.core.userdetails.UserDetailsService {

    // UserRepository üzerinden veritabanına erişerek kullanıcı bilgisi alınır
    @Autowired
    private UserRepository userRepository;

    /**
     * Belirtilen kullanıcı adına göre kullanıcıyı veritabanından getirir.
     * Eğer kullanıcı bulunamazsa UsernameNotFoundException fırlatılır.
     *
     * @param username Sisteme giriş yapmaya çalışan kullanıcının kullanıcı adı
     * @return Spring Security için uygun UserDetails nesnesi
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Kullanıcıyı veritabanında ara
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Veritabanından gelen kullanıcı bilgilerini Spring Security'nin UserDetails objesine çevir
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),         // Kullanıcı adı
                user.getPassword(),         // Şifre (şifrelenmiş)
                new ArrayList<>()           // Kullanıcının yetkileri (roller) — şimdilik boş
        );
    }
}
