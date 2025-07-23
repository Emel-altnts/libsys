package com.d_tech.libsys.config;

import com.d_tech.libsys.domain.model.User;
import com.d_tech.libsys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Uygulama başlatıldığında test kullanıcılarını otomatik olarak oluşturan sınıf.
 */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🔄 DataLoader çalışıyor - Kullanıcılar kontrol ediliyor...");

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

        // Test için admin kullanıcısının rollerini kontrol et
        userRepository.findByUsername("admin").ifPresent(admin -> {
            System.out.println("🔑 Admin kullanıcısı rolleri: " + admin.getRoles());
        });

        System.out.println("✅ DataLoader tamamlandı");
    }
}