package com.d_tech.libsys.service;

import com.d_tech.libsys.domain.model.User;
import com.d_tech.libsys.dto.SignupRequest;
import com.d_tech.libsys.dto.SignupResponse;
import com.d_tech.libsys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * UserServiceImpl - kullanıcı işlemleri için servis katmanı implementasyonu
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public SignupResponse registerUser(SignupRequest signupRequest) {
        try {
            // 1. Kullanıcı adı validasyonu
            if (signupRequest.getUsername() == null || signupRequest.getUsername().trim().isEmpty()) {
                return new SignupResponse("Kullanıcı adı boş olamaz!");
            }

            // 2. Şifre validasyonu
            if (signupRequest.getPassword() == null || signupRequest.getPassword().length() < 6) {
                return new SignupResponse("Şifre en az 6 karakter olmalıdır!");
            }

            // 3. Şifre onayı kontrolü
            if (!signupRequest.getPassword().equals(signupRequest.getConfirmPassword())) {
                return new SignupResponse("Şifreler eşleşmiyor!");
            }

            // 4. Kullanıcı adı benzersizlik kontrolü
            if (userRepository.existsByUsername(signupRequest.getUsername())) {
                return new SignupResponse("Bu kullanıcı adı zaten mevcut!");
            }

            // 5. Yeni kullanıcı oluştur
            User newUser = User.builder()
                    .username(signupRequest.getUsername().trim())
                    .password(passwordEncoder.encode(signupRequest.getPassword()))
                    .roles(Set.of("USER")) // Varsayılan rol
                    .build();

            // 6. Veritabanına kaydet
            userRepository.save(newUser);

            // 7. Başarı mesajı döndür
            return new SignupResponse("Kullanıcı başarıyla kaydedildi!");

        } catch (Exception e) {
            // Hata durumunda log ve kullanıcıya bilgi ver
            System.out.println("Kullanıcı kayıt hatası: " + e.getMessage());
            return new SignupResponse("Kayıt işlemi sırasında bir hata oluştu!");
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}
