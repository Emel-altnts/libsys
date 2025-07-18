package com.d_tech.libsys.repository;

import com.d_tech.libsys.domain.model.StockOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * StockOrderItem repository
 */
@Repository
public interface StockOrderItemRepository extends JpaRepository<StockOrderItem, Long> {

    /**
     * Sipariş ID'sine göre kalemleri bulur
     */
    List<StockOrderItem> findByStockOrderId(Long stockOrderId);

    /**
     * Kitap ID'sine göre sipariş kalemlerini bulur
     */
    List<StockOrderItem> findByBookId(Long bookId);

    /**
     * Tam teslimat alınmamış kalemleri bulur
     */
    @Query("SELECT soi FROM StockOrderItem soi WHERE soi.receivedQuantity < soi.quantity OR soi.receivedQuantity IS NULL")
    List<StockOrderItem> findPendingDeliveryItems();

    /**
     * Belirli siparişte belirli kitap var mı kontrolü
     */
    boolean existsByStockOrderIdAndBookId(Long stockOrderId, Long bookId);

    /**
     * Siparişteki toplam kalem sayısı
     */
    @Query("SELECT COUNT(soi) FROM StockOrderItem soi WHERE soi.stockOrder.id = :orderId")
    Long countByStockOrderId(@Param("orderId") Long orderId);
}

