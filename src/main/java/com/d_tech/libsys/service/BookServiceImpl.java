
package com.d_tech.libsys.service;

import com.d_tech.libsys.domain.model.Book;

import com.d_tech.libsys.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// Güncelleme ve silme işlemlerinden önce veritabanında ilgili ID’ye sahip bir kitap olup olmadığı kontrol edilir.
//Kitap bulunursa işlem yapılır, bulunmazsa boş sonuç döner.

@Service
public class BookServiceImpl implements BookService {

    @Autowired
    private BookRepository bookRepository; // Veritabanı işlemleri için repository'i kullanır


    // Tüm kitapları getir
    @Override
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }


    // ID ile kitap bul
    @Override
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }


    // Yeni kitap ekle veya güncelle
    @Override
    public Book saveBook(Book book) {
        return bookRepository.save(book);
    }


    // Mevcut kitabı güncelle
    @Override
    public Optional<Book> updateBook(Long id, Book bookDetails) {
        Optional<Book> optionalBook = bookRepository.findById(id);
        if (optionalBook.isPresent()) {
            // Gelen verilerle güncelle
            Book book = optionalBook.get();
            book.setTitle(bookDetails.getTitle());
            book.setAuthor(bookDetails.getAuthor());
            book.setYear(bookDetails.getYear());
            return Optional.of(bookRepository.save(book));
        }
        return Optional.empty();
    }

    // Kitabı sil (varsa)
    @Override
    public boolean deleteBook(Long id) {
        Optional<Book> optionalBook = bookRepository.findById(id);
        if (optionalBook.isPresent()) {
            bookRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
