package com.d_tech.libsys.service;

import com.d_tech.libsys.domain.model.Book;



import java.util.List;
import java.util.Optional;


// Book işlemleri için tanımlanan servis arayüzü
public interface BookService {
    List<Book> getAllBooks(); // Tüm kitapları listele
    Optional<Book> getBookById(Long id); // IDile kitap getir
    Book saveBook(Book book); //Kitap ekle
    Optional<Book> updateBook(Long id, Book book); //Kitap Güncelle
    boolean deleteBook(Long id); //Kitap Sil
}

