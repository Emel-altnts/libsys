package com.d_tech.libsys.controller;

import com.d_tech.libsys.domain.model.Book;
import com.d_tech.libsys.domain.model.BookStock;
import com.d_tech.libsys.service.BookService;
import com.d_tech.libsys.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 🚀 ENHANCED: Book Controller with Image Support and Rich Data
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final StockService stockService;

    /**
     * 🚀 ENHANCED: Tüm kitapları getir - stok bilgisi ile birlikte
     */
    @GetMapping
    public ResponseEntity<List<BookWithStockDto>> getAllBooks() {
        System.out.println("📚 Tüm kitaplar istendi (stok bilgisi ile)");

        try {
            List<Book> books = bookService.getAllBooks();

            List<BookWithStockDto> booksWithStock = books.stream()
                    .map(this::convertToBookWithStockDto)
                    .collect(Collectors.toList());

            System.out.println("✅ " + booksWithStock.size() + " kitap döndürüldü");
            return ResponseEntity.ok(booksWithStock);

        } catch (Exception e) {
            System.err.println("❌ Kitaplar getirme hatası: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 🚀 ENHANCED: ID ile kitap getir - stok bilgisi ile birlikte
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookWithStockDto> getBookById(@PathVariable Long id) {
        System.out.println("📖 Kitap detayı istendi: id=" + id);

        try {
            Optional<Book> bookOpt = bookService.getBookById(id);

            if (bookOpt.isPresent()) {
                BookWithStockDto bookWithStock = convertToBookWithStockDto(bookOpt.get());
                System.out.println("✅ Kitap bulundu: " + bookWithStock.getTitle());
                return ResponseEntity.ok(bookWithStock);
            } else {
                System.out.println("❌ Kitap bulunamadı: id=" + id);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            System.err.println("❌ Kitap detayı getirme hatası: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 🚀 NEW: Kategoriye göre kitapları getir
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<BookWithStockDto>> getBooksByCategory(@PathVariable String category) {
        System.out.println("📚 Kategoriye göre kitaplar istendi: category=" + category);

        try {
            List<Book> books = bookService.getAllBooks().stream()
                    .filter(book -> book.getCategory() != null &&
                            book.getCategory().toLowerCase().contains(category.toLowerCase()))
                    .collect(Collectors.toList());

            List<BookWithStockDto> booksWithStock = books.stream()
                    .map(this::convertToBookWithStockDto)
                    .collect(Collectors.toList());

            System.out.println("✅ " + booksWithStock.size() + " kitap bulundu (kategori: " + category + ")");
            return ResponseEntity.ok(booksWithStock);

        } catch (Exception e) {
            System.err.println("❌ Kategori kitapları getirme hatası: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 🚀 NEW: Yazar ile arama
     */
    @GetMapping("/search/author/{author}")
    public ResponseEntity<List<BookWithStockDto>> searchByAuthor(@PathVariable String author) {
        System.out.println("🔍 Yazar ile arama: author=" + author);

        try {
            List<Book> books = bookService.getAllBooks().stream()
                    .filter(book -> book.getAuthor() != null &&
                            book.getAuthor().toLowerCase().contains(author.toLowerCase()))
                    .collect(Collectors.toList());

            List<BookWithStockDto> booksWithStock = books.stream()
                    .map(this::convertToBookWithStockDto)
                    .collect(Collectors.toList());

            System.out.println("✅ " + booksWithStock.size() + " kitap bulundu (yazar: " + author + ")");
            return ResponseEntity.ok(booksWithStock);

        } catch (Exception e) {
            System.err.println("❌ Yazar arama hatası: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 🚀 NEW: Kitap başlığı ile arama
     */
    @GetMapping("/search/title/{title}")
    public ResponseEntity<List<BookWithStockDto>> searchByTitle(@PathVariable String title) {
        System.out.println("🔍 Başlık ile arama: title=" + title);

        try {
            List<Book> books = bookService.getAllBooks().stream()
                    .filter(book -> book.getTitle() != null &&
                            book.getTitle().toLowerCase().contains(title.toLowerCase()))
                    .collect(Collectors.toList());

            List<BookWithStockDto> booksWithStock = books.stream()
                    .map(this::convertToBookWithStockDto)
                    .collect(Collectors.toList());

            System.out.println("✅ " + booksWithStock.size() + " kitap bulundu (başlık: " + title + ")");
            return ResponseEntity.ok(booksWithStock);

        } catch (Exception e) {
            System.err.println("❌ Başlık arama hatası: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 🚀 NEW: Genel arama (başlık, yazar, kategori)
     */
    @GetMapping("/search")
    public ResponseEntity<List<BookWithStockDto>> searchBooks(@RequestParam String q) {
        System.out.println("🔍 Genel arama: query=" + q);

        try {
            String query = q.toLowerCase();
            List<Book> books = bookService.getAllBooks().stream()
                    .filter(book ->
                            (book.getTitle() != null && book.getTitle().toLowerCase().contains(query)) ||
                                    (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(query)) ||
                                    (book.getCategory() != null && book.getCategory().toLowerCase().contains(query)) ||
                                    (book.getDescription() != null && book.getDescription().toLowerCase().contains(query))
                    )
                    .collect(Collectors.toList());

            List<BookWithStockDto> booksWithStock = books.stream()
                    .map(this::convertToBookWithStockDto)
                    .collect(Collectors.toList());

            System.out.println("✅ " + booksWithStock.size() + " kitap bulundu (genel arama: " + q + ")");
            return ResponseEntity.ok(booksWithStock);

        } catch (Exception e) {
            System.err.println("❌ Genel arama hatası: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 🚀 NEW: Mevcut kategorileri listele
     */
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        System.out.println("📂 Kategoriler istendi");

        try {
            List<String> categories = bookService.getAllBooks().stream()
                    .map(Book::getCategory)
                    .filter(category -> category != null && !category.trim().isEmpty())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            System.out.println("✅ " + categories.size() + " kategori bulundu");
            return ResponseEntity.ok(categories);

        } catch (Exception e) {
            System.err.println("❌ Kategori listesi getirme hatası: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 🚀 NEW: İstatistikler endpoint'i
     */
    @GetMapping("/statistics")
    public ResponseEntity<BookStatisticsDto> getBookStatistics() {
        System.out.println("📊 Kitap istatistikleri istendi");

        try {
            List<Book> allBooks = bookService.getAllBooks();

            long totalBooks = allBooks.size();
            long totalCategories = allBooks.stream()
                    .map(Book::getCategory)
                    .filter(category -> category != null && !category.trim().isEmpty())
                    .distinct()
                    .count();

            long totalAuthors = allBooks.stream()
                    .map(Book::getAuthor)
                    .filter(author -> author != null && !author.trim().isEmpty())
                    .distinct()
                    .count();

            // En popüler kategori
            Map<String, Long> categoryCount = allBooks.stream()
                    .filter(book -> book.getCategory() != null)
                    .collect(Collectors.groupingBy(Book::getCategory, Collectors.counting()));

            String mostPopularCategory = categoryCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Bilinmeyen");

            BookStatisticsDto stats = BookStatisticsDto.builder()
                    .totalBooks(totalBooks)
                    .totalCategories(totalCategories)
                    .totalAuthors(totalAuthors)
                    .mostPopularCategory(mostPopularCategory)
                    .averageYear(allBooks.stream()
                            .filter(book -> book.getYear() > 0)
                            .mapToInt(Book::getYear)
                            .average()
                            .orElse(0.0))
                    .build();

            System.out.println("✅ İstatistikler hazırlandı: " + totalBooks + " kitap");
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            System.err.println("❌ İstatistik hesaplama hatası: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Kitap oluştur
     */
    @PostMapping
    public ResponseEntity<BookWithStockDto> createBook(@RequestBody Book book) {
        System.out.println("📝 Yeni kitap oluşturuluyor: " + book.getTitle());

        try {
            Book savedBook = bookService.saveBook(book);
            BookWithStockDto bookWithStock = convertToBookWithStockDto(savedBook);

            System.out.println("✅ Kitap oluşturuldu: id=" + savedBook.getId());
            return ResponseEntity.ok(bookWithStock);

        } catch (Exception e) {
            System.err.println("❌ Kitap oluşturma hatası: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Kitap güncelle
     */
    @PutMapping("/{id}")
    public ResponseEntity<BookWithStockDto> updateBook(@PathVariable Long id, @RequestBody Book book) {
        System.out.println("✏️ Kitap güncelleniyor: id=" + id);

        try {
            Optional<Book> updatedBookOpt = bookService.updateBook(id, book);

            if (updatedBookOpt.isPresent()) {
                BookWithStockDto bookWithStock = convertToBookWithStockDto(updatedBookOpt.get());
                System.out.println("✅ Kitap güncellendi: " + bookWithStock.getTitle());
                return ResponseEntity.ok(bookWithStock);
            } else {
                System.out.println("❌ Güncellenecek kitap bulunamadı: id=" + id);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            System.err.println("❌ Kitap güncelleme hatası: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Kitap sil
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        System.out.println("🗑️ Kitap siliniyor: id=" + id);

        try {
            boolean deleted = bookService.deleteBook(id);
            if (deleted) {
                System.out.println("✅ Kitap silindi: id=" + id);
                return ResponseEntity.noContent().build();
            } else {
                System.out.println("❌ Silinecek kitap bulunamadı: id=" + id);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            System.err.println("❌ Kitap silme hatası: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 🚀 HELPER: Book'u BookWithStockDto'ya çevir
     */
    private BookWithStockDto convertToBookWithStockDto(Book book) {
        // Stok bilgisini getir
        Optional<BookStock> stockOpt = stockService.getBookStock(book.getId());

        return BookWithStockDto.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .year(book.getYear())
                .imageUrl(book.getImageUrlOrDefault()) // Default image varsa onu kullan
                .description(book.getDescription())
                .shortDescription(book.getShortDescription())
                .isbn(book.getIsbn())
                .pageCount(book.getPageCount())
                .category(book.getCategory())
                .publisher(book.getPublisher())
                .language(book.getLanguage())
                .fullTitle(book.getFullTitle())
                .displayYear(book.getDisplayYear())
                // Stok bilgileri
                .hasStock(stockOpt.isPresent())
                .currentQuantity(stockOpt.map(BookStock::getCurrentQuantity).orElse(0))
                .minimumQuantity(stockOpt.map(BookStock::getMinimumQuantity).orElse(0))
                .unitPrice(stockOpt.map(BookStock::getUnitPrice).orElse(null))
                .stockStatus(stockOpt.map(stock -> stock.getStatus().toString()).orElse("NO_STOCK"))
                .supplierName(stockOpt.map(BookStock::getSupplierName).orElse(null))
                .isRestockNeeded(stockOpt.map(BookStock::isRestockNeeded).orElse(false))
                .recommendedOrderQuantity(stockOpt.map(BookStock::getRecommendedOrderQuantity).orElse(0))
                .build();
    }

    // 🚀 DTO Classes

    /**
     * 🚀 Frontend için zengin kitap bilgisi DTO'su
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BookWithStockDto {
        // Temel kitap bilgileri
        private Long id;
        private String title;
        private String author;
        private Integer year;
        private String imageUrl;
        private String description;
        private String shortDescription;
        private String isbn;
        private Integer pageCount;
        private String category;
        private String publisher;
        private String language;

        // Computed fields
        private String fullTitle;
        private String displayYear;

        // Stok bilgileri
        private Boolean hasStock;
        private Integer currentQuantity;
        private Integer minimumQuantity;
        private java.math.BigDecimal unitPrice;
        private String stockStatus;
        private String supplierName;
        private Boolean isRestockNeeded;
        private Integer recommendedOrderQuantity;
    }

    /**
     * 🚀 Kitap istatistikleri DTO'su
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BookStatisticsDto {
        private Long totalBooks;
        private Long totalCategories;
        private Long totalAuthors;
        private String mostPopularCategory;
        private Double averageYear;
        private Long booksWithImages;
        private Long booksWithStock;
    }
}