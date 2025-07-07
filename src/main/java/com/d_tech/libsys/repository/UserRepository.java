package com.d_tech.libsys.repository;

import com.d_tech.libsys.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository interface'i, User entity'si için veritabanı işlemlerini sağlar.
 * Spring Data JPA tarafından otomatik olarak implement edilir.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Kullanıcı adına göre kullanıcıyı bulur.
     * Authentication sürecinde kullanılır.
     *
     * @param username Aranacak kullanıcı adı
     * @return Kullanıcı bulunursa Optional<User>, bulunamazsa Optional.empty()
     */
    Optional<User> findByUsername(String username);

    /**
     * Kullanıcı adının sistemde var olup olmadığını kontrol eder.
     *
     * @param username Kontrol edilecek kullanıcı adı
     * @return Varsa true, yoksa false
     */
    boolean existsByUsername(String username);
}