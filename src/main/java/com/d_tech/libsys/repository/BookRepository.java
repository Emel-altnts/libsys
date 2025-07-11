package com.d_tech.libsys.repository;

import com.d_tech.libsys.domain.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
}