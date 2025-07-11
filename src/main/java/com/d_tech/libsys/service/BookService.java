package com.d_tech.libsys.service;

import com.d_tech.libsys.domain.model.Book;
import java.util.List;
import java.util.Optional;

public interface BookService {
    List<Book> getAllBooks();
    Optional<Book> getBookById(Long id);
    Book saveBook(Book book);
    Optional<Book> updateBook(Long id, Book book);
    boolean deleteBook(Long id);
}