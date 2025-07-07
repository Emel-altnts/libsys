package com.d_tech.libsys.controller;

import com.d_tech.libsys.domain.model.User;
import com.d_tech.libsys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bu controller, kullanıcılarla ilgili işlemleri (listeleme, profil görüntüleme vs.) yönetir.
 * Güvenlik için JWT zorunludur ve gerekirse rol bazlı erişim de sağlanabilir.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * Sistemdeki tüm kullanıcıları getirir.
     * Bu endpoint sadece ADMIN rolündeki kullanıcılar tarafından erişilebilir.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    /**
     * Belirli bir kullanıcıyı ID’ye göre getirir.
     * Bu işlem sadece kimliği doğrulanmış kullanıcılar için geçerlidir.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Kullanıcı silme işlemi — sadece ADMIN’ler yapabilir.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }


}
