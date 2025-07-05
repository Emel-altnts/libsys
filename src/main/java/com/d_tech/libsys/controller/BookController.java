package com.d_tech.libsys.controller;

import com.d_tech.libsys.domain.model.Book;
import com.d_tech.libsys.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;


//HTTP isteklerini karşılar ve uygun servisi çağırarak yanıt üretir.
// kullanıcı /api/books isteği gönderirse, Controller bu isteği getAllBooks() metoduna yönlendirir.

@RestController
@RequestMapping("/api/books") // Tüm istekler /api/books adresinden başlar
public class BookController {

    @Autowired
    private BookService bookService;  // BookService katmanını otomatik olarak projeye enjekte eder

    @GetMapping
    public List<Book> getAllBooks() {   // Tüm kitapları listeleyen Get isteği
        return bookService.getAllBooks();
    }

    // Belirli bir kitabı ID'sine göre getiren GET isteği
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        return bookService.getBookById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    // Kitap bulunursa 200 OK, bulunmazsa 404 Not Found döner


    // Yeni kitap eklemek için kullanılan POST isteği

    @PostMapping
    public Book createBook(@RequestBody Book book) {
        return bookService.saveBook(book);
    }


    // Var olan kitabı güncellemek için PUT isteği

    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(@PathVariable Long id, @RequestBody Book book) {
        return bookService.updateBook(id, book)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Kitap silmek için kullanılan DELETE isteği

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        boolean deleted = bookService.deleteBook(id);
        if (deleted) {
            return ResponseEntity.noContent().build(); // Başarıyla silindiyse 204 No Content döner
        }
        return ResponseEntity.notFound().build(); // Silinmek istenen kitap yoksa 404 Not Found döner
    }
}
