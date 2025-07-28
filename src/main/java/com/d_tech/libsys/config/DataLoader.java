package com.d_tech.libsys.config;

import com.d_tech.libsys.domain.model.Book;
import com.d_tech.libsys.domain.model.BookStock;
import com.d_tech.libsys.domain.model.User;
import com.d_tech.libsys.repository.BookRepository;
import com.d_tech.libsys.repository.BookStockRepository;
import com.d_tech.libsys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/**
 * 🚀 ENHANCED: Uygulama başlatıldığında test kullanıcılarını ve örnek kitapları otomatik olarak oluşturan sınıf.
 */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookStockRepository bookStockRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🔄 Enhanced DataLoader çalışıyor - Kullanıcılar ve kitaplar kontrol ediliyor...");

        // 1. Kullanıcıları oluştur
        createUsers();

        // 2. Örnek kitapları oluştur
        createSampleBooks();

        System.out.println("✅ Enhanced DataLoader tamamlandı");
    }

    /**
     * 🚀 Test kullanıcılarını oluştur
     */
    private void createUsers() {
        // Test kullanıcısı oluştur
        if (!userRepository.existsByUsername("testuser")) {
            User testUser = User.builder()
                    .username("testuser")
                    .password(passwordEncoder.encode("test1234"))
                    .roles(Set.of("USER"))
                    .build();
            userRepository.save(testUser);
            System.out.println("✅ Test kullanıcısı oluşturuldu: testuser/test1234 (Roles: USER)");
        } else {
            System.out.println("ℹ️ Test kullanıcısı zaten mevcut: testuser");
        }

        // Admin kullanıcısı oluştur
        if (!userRepository.existsByUsername("admin")) {
            User adminUser = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .roles(Set.of("ADMIN", "USER"))
                    .build();
            userRepository.save(adminUser);
            System.out.println("✅ Admin kullanıcısı oluşturuldu: admin/admin123 (Roles: ADMIN, USER)");
        } else {
            System.out.println("ℹ️ Admin kullanıcısı zaten mevcut: admin");
        }

        // Kullanıcı sayısını kontrol et
        long userCount = userRepository.count();
        System.out.println("📊 Toplam kullanıcı sayısı: " + userCount);
    }

    /**
     * 🚀 NEW: Örnek kitapları oluştur
     */
    private void createSampleBooks() {
        // Mevcut kitap sayısını kontrol et
        long bookCount = bookRepository.count();
        if (bookCount > 0) {
            System.out.println("ℹ️ Kitaplar zaten mevcut - toplam: " + bookCount);
            return;
        }

        System.out.println("📚 Örnek kitaplar oluşturuluyor...");

        // Türk Klasikleri
        createBookWithStock(
                "Sinekli Bakkal",
                "Halide Edib Adıvar",
                1936,
                "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=300&h=400&fit=crop&auto=format&q=80",
                "Türk edebiyatının önemli eserlerinden biri olan bu roman, toplumsal değişimleri ele alır.",
                "9789753638647",
                280,
                "Roman",
                "İş Bankası Kültür Yayınları",
                "Turkish",
                25,
                new BigDecimal("18.50"),
                "Türk Klasikleri Ltd.",
                "+90 212 555 0101"
        );

        createBookWithStock(
                "Tutunamayanlar",
                "Oğuz Atay",
                1971,
                "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=300&h=400&fit=crop&auto=format&q=80",
                "Modern Türk edebiyatının başyapıtlarından biri olarak kabul edilen felsefi roman.",
                "9789750718056",
                724,
                "Roman",
                "İletişim Yayınları",
                "Turkish",
                15,
                new BigDecimal("32.00"),
                "Modern Edebiyat A.Ş.",
                "+90 212 555 0102"
        );

        // Dünya Klasikleri
        createBookWithStock(
                "1984",
                "George Orwell",
                1949,
                "https://images.unsplash.com/photo-1495640388908-05fa85288e61?w=300&h=400&fit=crop&auto=format&q=80",
                "Totaliter rejimleri eleştiren distopik bir roman. Big Brother'ın izlediği toplumu anlatır.",
                "9780451524935",
                328,
                "Distopya",
                "Signet Classics",
                "Turkish",
                30,
                new BigDecimal("22.75"),
                "Dünya Klasikleri Ltd.",
                "+90 212 555 0103"
        );

        createBookWithStock(
                "Suç ve Ceza",
                "Fyodor Dostoyevski",
                1866,
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&h=400&fit=crop&auto=format&q=80",
                "Rus edebiyatının başyapıtlarından biri. Psikolojik derinlikli suç romanı.",
                "9789750719721",
                671,
                "Klasik Roman",
                "İletişim Yayınları",
                "Turkish",
                18,
                new BigDecimal("28.90"),
                "Rus Edebiyatı A.Ş.",
                "+90 212 555 0104"
        );

        // Bilim Kurgu
        createBookWithStock(
                "Dune",
                "Frank Herbert",
                1965,
                "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=300&h=400&fit=crop&auto=format&q=80",
                "Bilim kurgu edebiyatının şaheseri. Arrakis gezegenindeki epik hikaye.",
                "9780441172719",
                688,
                "Bilim Kurgu",
                "Ace Books",
                "Turkish",
                22,
                new BigDecimal("35.50"),
                "SciFi Yayınları",
                "+90 212 555 0105"
        );

        // Fantastik
        createBookWithStock(
                "Yüzüklerin Efendisi: Yüzük Kardeşliği",
                "J.R.R. Tolkien",
                1954,
                "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=300&h=400&fit=crop&auto=format&q=80",
                "Fantasy edebiyatının kilometre taşı. Orta Dünya'daki büyük macera.",
                "9780547928210",
                423,
                "Fantasy",
                "Houghton Mifflin Harcourt",
                "Turkish",
                35,
                new BigDecimal("29.99"),
                "Fantasy World Ltd.",
                "+90 212 555 0106"
        );

        // Çağdaş Türk Edebiyatı
        createBookWithStock(
                "Masumiyet Müzesi",
                "Orhan Pamuk",
                2008,
                "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=300&h=400&fit=crop&auto=format&q=80",
                "Nobel ödüllü yazarın İstanbul'da geçen aşk hikayesi. Nostalji dolu bir anlatım.",
                "9789750827297",
                592,
                "Çağdaş Roman",
                "İletişim Yayınları",
                "Turkish",
                40,
                new BigDecimal("26.50"),
                "Nobel Yayınları",
                "+90 212 555 0107"
        );

        // Felsefe
        createBookWithStock(
                "Böyle Buyurdu Zerdüşt",
                "Friedrich Nietzsche",
                1883,
                "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=300&h=400&fit=crop&auto=format&q=80",
                "Nietzsche'nin en önemli eseri. Üstinsan kavramı ve ahlak eleştirisi.",
                "9789750719035",
                352,
                "Felsefe",
                "İletişim Yayınları",
                "Turkish",
                12,
                new BigDecimal("24.75"),
                "Felsefe Dünyası",
                "+90 212 555 0108"
        );

        // Tarih
        createBookWithStock(
                "Nutuk",
                "Mustafa Kemal Atatürk",
                1927,
                "https://images.unsplash.com/photo-1589829085413-56de8ae18c73?w=300&h=400&fit=crop&auto=format&q=80",
                "Atatürk'ün Kurtuluş Savaşı ve Cumhuriyet'in kuruluşunu anlattığı tarihi eser.",
                "9789751410016",
                645,
                "Tarih",
                "Türk Tarih Kurumu",
                "Turkish",
                50,
                new BigDecimal("15.00"),
                "Tarih Yayınları",
                "+90 212 555 0109"
        );

        // Polisiye
        createBookWithStock(
                "Sherlock Holmes: Baskerville Tazıları",
                "Arthur Conan Doyle",
                1902,
                "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=300&h=400&fit=crop&auto=format&q=80",
                "Sherlock Holmes serisinin en ünlü hikayelerinden biri. Gizemli bir cinayet vakası.",
                "9780141034355",
                256,
                "Polisiye",
                "Penguin Classics",
                "Turkish",
                28,
                new BigDecimal("19.90"),
                "Mystery Books Ltd.",
                "+90 212 555 0110"
        );

        long finalBookCount = bookRepository.count();
        System.out.println("✅ Örnek kitaplar oluşturuldu - toplam: " + finalBookCount);
    }

    /**
     * 🚀 Kitap ve stok kaydını birlikte oluştur
     */
    private void createBookWithStock(String title, String author, int year, String imageUrl,
                                     String description, String isbn, int pageCount, String category,
                                     String publisher, String language, int stockQuantity,
                                     BigDecimal unitPrice, String supplierName, String supplierContact) {
        try {
            // Kitabı oluştur
            Book book = Book.builder()
                    .title(title)
                    .author(author)
                    .year(year)
                    .imageUrl(imageUrl)
                    .description(description)
                    .isbn(isbn)
                    .pageCount(pageCount)
                    .category(category)
                    .publisher(publisher)
                    .language(language)
                    .build();

            Book savedBook = bookRepository.save(book);

            // Stok kaydını oluştur
            BookStock stock = BookStock.builder()
                    .book(savedBook)
                    .currentQuantity(stockQuantity)
                    .minimumQuantity(5) // Minimum stok: 5
                    .maximumQuantity(100) // Maksimum stok: 100
                    .unitPrice(unitPrice)
                    .supplierName(supplierName)
                    .supplierContact(supplierContact)
                    .build();

            bookStockRepository.save(stock);

            System.out.println("✅ Kitap ve stok oluşturuldu: " + title + " (Stok: " + stockQuantity + ")");

        } catch (Exception e) {
            System.err.println("❌ Kitap oluşturma hatası: " + title + " - " + e.getMessage());
        }
    }
}