package com.d_tech.libsys.repository;

import com.d_tech.libsys.domain.model.RegistrationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * RegistrationEvent repository
 */
@Repository
public interface RegistrationEventRepository extends JpaRepository<RegistrationEvent, Long> {

    /**
     * Event ID ile kayıt event'ini bulur
     */
    Optional<RegistrationEvent> findByEventId(String eventId);

    /**
     * Kullanıcı adına göre event'leri bulur
     */
    List<RegistrationEvent> findByUsernameOrderByCreatedAtDesc(String username);

    /**
     * Duruma göre event'leri bulur
     */
    List<RegistrationEvent> findByStatus(RegistrationEvent.EventStatus status);

    /**
     * Belirli bir tarihten sonra oluşturulan event'leri bulur
     */
    List<RegistrationEvent> findByCreatedAtAfter(LocalDateTime after);

    /**
     * Pending ve Processing durumundaki event'leri bulur (cleanup için)
     */
    @Query("SELECT e FROM RegistrationEvent e WHERE e.status IN ('PENDING', 'PROCESSING') AND e.createdAt < :cutoffTime")
    List<RegistrationEvent> findStaleEvents(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Event ID'nin var olup olmadığını kontrol eder
     */
    boolean existsByEventId(String eventId);
}