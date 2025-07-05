
package com.d_tech.libsys.repository;

import com.d_tech.libsys.domain.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

// JpaRepository, Spring Data JPA tarafından sağlanan hazır bir arayüz
// JpaRepository arayüzü, Book entity'si için CRUD işlemleri sağlar

// Ekstra metot yazmadan hazır birçok sorgu kullanılabilir
// findAll(), findById(), save(), deleteById() gibi tüm temel CRUD işlemleri otomatik yapılır.

public interface BookRepository extends JpaRepository<Book, Long> {
}
