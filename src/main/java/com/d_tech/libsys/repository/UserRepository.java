package com.d_tech.libsys.repository;

import com.d_tech.libsys.domain.model.User; // Veritabanındaki kullanıcıyı temsil eden entity
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * UserRepository, User entity'si için CRUD işlemlerini sağlayan bir Spring Data JPA arayüzüdür.
 *
 * JpaRepository<User, Long>:
 * - User: Entity tipi
 * - Long: User entity'sindeki ID alanının veri tipi
 *
 * Spring otomatik olarak bu interface için çalışma zamanında implementasyon oluşturur.
 * Ekstra SQL yazmadan veri çekme, kaydetme, silme gibi işlemler yapılabilir.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Kullanıcı adına göre kullanıcıyı bulur.
     * Spring Data JPA, method ismine göre otomatik SQL sorgusu oluşturur.
     *
     * @param username Kullanıcı adı (unique olmalı)
     * @return Kullanıcı bulunduysa Optional<User>, bulunamadıysa boş
     */
    Optional<User> findByUsername(String username);
}


