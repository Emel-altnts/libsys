package com.d_tech.libsys.config;

import com.d_tech.libsys.domain.model.Book;
import com.d_tech.libsys.domain.model.BookStock;
import com.d_tech.libsys.domain.model.User;
import com.d_tech.libsys.repository.BookRepository;
import com.d_tech.libsys.repository.BookStockRepository;
import com.d_tech.libsys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Full Featured DataLoader - H2 database ile tüm örnek veriler
 * Books + BookStocks + Users + Test Data
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BookStockRepository bookStockRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 FULL FEATURED DataLoader başlatılıyor - H2 + Kafka + Tüm Özellikler");

        createUsers();
        createBooksWithStock();

        log.info("✅ FULL FEATURED DataLoader tamamlandı!");
        log.info("📊 Toplam kullanıcı: {}", userRepository.count());
        log.info("📚 Toplam kitap: {}", bookRepository.count());
        log.info("📦 Toplam stok kaydı: {}", bookStockRepository.count());
    }

    /**
     * Test kullanıcıları oluştur - Farklı roller ile
     */
    private void createUsers() {
        log.info("👥 Kullanıcılar oluşturuluyor...");

        // Test kullanıcısı
        if (!userRepository.existsByUsername("test")) {
            User testUser = User.builder()
                    .username("test")
                    .password(passwordEncoder.encode("123456"))
                    .roles(Set.of("USER"))
                    .build();
            userRepository.save(testUser);
            log.info("✅ Test kullanıcısı: test/123456 (USER)");
        }

        // Admin kullanıcısı
        if (!userRepository.existsByUsername("admin")) {
            User adminUser = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin"))
                    .roles(Set.of("ADMIN", "USER"))
                    .build();
            userRepository.save(adminUser);
            log.info("✅ Admin kullanıcısı: admin/admin (ADMIN, USER)");
        }

        // Frontend test kullanıcısı
        if (!userRepository.existsByUsername("frontend")) {
            User frontendUser = User.builder()
                    .username("frontend")
                    .password(passwordEncoder.encode("frontend123"))
                    .roles(Set.of("USER"))
                    .build();
            userRepository.save(frontendUser);
            log.info("✅ Frontend test kullanıcısı: frontend/frontend123 (USER)");
        }

        // Manager kullanıcısı
        if (!userRepository.existsByUsername("manager")) {
            User managerUser = User.builder()
                    .username("manager")
                    .password(passwordEncoder.encode("manager123"))
                    .roles(Set.of("ADMIN", "USER"))
                    .build();
            userRepository.save(managerUser);
            log.info("✅ Manager kullanıcısı: manager/manager123 (ADMIN, USER)");
        }
    }

    /**
     * Kitaplar ve stok bilgileri oluştur
     */
    private void createBooksWithStock() {
        if (bookRepository.count() > 0) {
            log.info("ℹ️ Kitaplar zaten mevcut - toplam: {}", bookRepository.count());
            return;
        }

        log.info("📚 Kitaplar ve stok bilgileri oluşturuluyor...");

        // Türk Klasikleri
        createBookWithStock("Sinekli Bakkal", "Halide Edib Adıvar", 1936,
                "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=300&h=400&fit=crop",
                "Türk edebiyatının önemli eserlerinden biri olan bu roman, toplumsal değişimleri ele alır.",
                "9789753638647", 280, "Türk Klasikleri", "İş Bankası Kültür",
                25, new BigDecimal("18.50"), "Türk Klasikleri Ltd.", "+90 212 555 0101");

        createBookWithStock("Tutunamayanlar", "Oğuz Atay", 1971,
                "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=300&h=400&fit=crop",
                "Modern Türk edebiyatının başyapıtlarından biri olarak kabul edilen felsefi roman.",
                "9789750718056", 724, "Türk Klasikleri", "İletişim Yayınları",
                15, new BigDecimal("32.00"), "Modern Edebiyat A.Ş.", "+90 212 555 0102");

        // Dünya Klasikleri
        createBookWithStock("1984", "George Orwell", 1949,
                "https://images.unsplash.com/photo-1495640388908-05fa85288e61?w=300&h=400&fit=crop",
                "Totaliter rejimleri eleştiren distopik bir roman. Big Brother'ın izlediği toplumu anlatır.",
                "9780451524935", 328, "Distopya", "Signet Classics",
                30, new BigDecimal("22.75"), "Dünya Klasikleri Ltd.", "+90 212 555 0103");

        createBookWithStock("Suç ve Ceza", "Fyodor Dostoyevski", 1866,
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&h=400&fit=crop",
                "Rus edebiyatının başyapıtlarından biri. Psikolojik derinlikli suç romanı.",
                "9789750719721", 671, "Klasik", "İletişim Yayınları",
                18, new BigDecimal("28.90"), "Rus Edebiyatı A.Ş.", "+90 212 555 0104");

        // Bilim Kurgu
        createBookWithStock("Dune", "Frank Herbert", 1965,
                "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=300&h=400&fit=crop",
                "Bilim kurgu edebiyatının şaheseri. Arrakis gezegenindeki epik hikaye.",
                "9780441172719", 688, "Bilim Kurgu", "Ace Books",
                22, new BigDecimal("35.50"), "SciFi Yayınları", "+90 212 555 0105");

        // Fantastik
        createBookWithStock("Yüzüklerin Efendisi: Yüzük Kardeşliği", "J.R.R. Tolkien", 1954,
                "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=300&h=400&fit=crop",
                "Fantasy edebiyatının kilometre taşı. Orta Dünya'daki büyük macera.",
                "9780547928210", 423, "Fantasy", "Houghton Mifflin",
                35, new BigDecimal("29.99"), "Fantasy World Ltd.", "+90 212 555 0106");

        // Çağdaş Türk Edebiyatı
        createBookWithStock("Masumiyet Müzesi", "Orhan Pamuk", 2008,
                "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=300&h=400&fit=crop",
                "Nobel ödüllü yazarın İstanbul'da geçen aşk hikayesi. Nostalgi dolu bir anlatım.",
                "9789750827297", 592, "Çağdaş", "İletişim Yayınları",
                40, new BigDecimal("26.50"), "Nobel Yayınları", "+90 212 555 0107");

        // Felsefe
        createBookWithStock("Böyle Buyurdu Zerdüşt", "Friedrich Nietzsche", 1883,
                "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=300&h=400&fit=crop",
                "Nietzsche'nin en önemli eseri. Üstinsan kavramı ve ahlak eleştirisi.",
                "9789750719035", 352, "Felsefe", "İletişim Yayınları",
                12, new BigDecimal("24.75"), "Felsefe Dünyası", "+90 212 555 0108");

        // Tarih
        createBookWithStock("Nutuk", "Mustafa Kemal Atatürk", 1927,
                "https://images.unsplash.com/photo-1589829085413-56de8ae18c73?w=300&h=400&fit=crop",
                "Atatürk'ün Kurtuluş Savaşı ve Cumhuriyet'in kuruluşunu anlattığı tarihi eser.",
                "9789751410016", 645, "Tarih", "Türk Tarih Kurumu",
                50, new BigDecimal("15.00"), "Tarih Yayınları", "+90 212 555 0109");

        // Polisiye
        createBookWithStock("Sherlock Holmes: Baskerville Tazıları", "Arthur Conan Doyle", 1902,
                "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=300&h=400&fit=crop",
                "Sherlock Holmes serisinin en ünlü hikayelerinden biri. Gizemli bir cinayet vakası.",
                "9780141034355", 256, "Polisiye", "Penguin Classics",
                28, new BigDecimal("19.90"), "Mystery Books Ltd.", "+90 212 555 0110");

        // Düşük stoklu kitaplar (test için)
        createBookWithStock("Stoksuz Kitap", "Test Yazar", 2023,
                "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=300&h=400&fit=crop",
                "Bu kitap stok testi için oluşturulmuştur.",
                "9780000000001", 100, "Test", "Test Yayınları",
                2, new BigDecimal("10.00"), "Test Supplier", "+90 212 555 0111");

        createBookWithStock("Kritik Stok", "Test Yazar 2", 2023,
                "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=300&h=400&fit=crop",
                "Bu kitap kritik stok seviyesinde.",
                "9780000000002", 150, "Test", "Test Yayınları",
                5, new BigDecimal("12.00"), "Test Supplier", "+90 212 555 0112");

        log.info("✅ {} kitap ve stok kaydı oluşturuldu", bookRepository.count());
    }

    /**
     * Kitap ve stok kaydını birlikte oluştur
     */
    private void createBookWithStock(String title, String author, int year, String imageUrl,
                                     String description, String isbn, int pageCount, String category,
                                     String publisher, int stockQuantity, BigDecimal unitPrice,
                                     String supplierName, String supplierContact) {
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
                    .language("Turkish")
                    .build();

            Book savedBook = bookRepository.save(book);

            // Stok kaydını oluştur
            BookStock stock = BookStock.builder()
                    .book(savedBook)
                    .currentQuantity(stockQuantity)
                    .minimumQuantity(stockQuantity < 10 ? 2 : 10) // Düşük stok testi için
                    .maximumQuantity(100)
                    .unitPrice(unitPrice)
                    .supplierName(supplierName)
                    .supplierContact(supplierContact)
                    .build();

            bookStockRepository.save(stock);

            log.debug("📖 Kitap + Stok: {} (Stok: {})", title, stockQuantity);

        } catch (Exception e) {
            log.error("❌ Kitap oluşturma hatası: {} - {}", title, e.getMessage());
        }
    }
}