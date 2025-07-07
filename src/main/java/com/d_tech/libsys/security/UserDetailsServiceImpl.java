package com.d_tech.libsys.security;

import com.d_tech.libsys.domain.model.User;
import com.d_tech.libsys.dto.UserDto;
import com.d_tech.libsys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UserDetailsServiceImpl sınıfı, Spring Security'nin kullanıcıyı kimlik doğrulama (authentication) sürecinde
 * veritabanından bulması için gerekli olan UserDetailsService arayüzünün implementasyonudur.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void saveUser(UserDto userDto) {
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setRoles(Set.of("USER")); // varsayılan rol
        userRepository.save(user);
    }

    /**
     * Belirtilen kullanıcı adına göre kullanıcıyı veritabanından getirir.
     * Rolleri de dahil ederek UserDetails nesnesini oluşturur.
     *
     * @param username Sisteme giriş yapmaya çalışan kullanıcının kullanıcı adı
     * @return Spring Security için uygun UserDetails nesnesi
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Kullanıcıyı veritabanında ara
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Kullanıcının rollerini Spring Security'nin anlayacağı yetkilere çevir
        Collection<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role)) // ROLE_ prefix'i ekle
                .collect(Collectors.toList());

        // UserDetails nesnesini oluştur
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities // Artık roller dahil
        );
    }
}