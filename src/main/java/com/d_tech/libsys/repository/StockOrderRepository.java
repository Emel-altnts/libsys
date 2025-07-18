package com.d_tech.libsys.repository;

import com.d_tech.libsys.domain.model.StockOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * StockOrder repository
 */
@Repository
public interface StockOrderRepository extends JpaRepository<StockOrder, Long> {

    /**
     * Sipariş numarasına göre bulur
     */
    Optional<StockOrder> findByOrderNumber(String orderNumber);

    /**
     * Duruma göre siparişleri bulur
     */
    List<StockOrder> findByStatus(StockOrder.OrderStatus status);

    /**
     * Tedarikçiye göre siparişleri bulur
     */
    List<StockOrder> findBySupplierNameContainingIgnoreCase(String supplierName);

    /**
     * Tarih aralığında verilen siparişleri bulur
     */
    @Query("SELECT so FROM StockOrder so WHERE so.orderDate BETWEEN :startDate AND :endDate")
    List<StockOrder> findByOrderDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Bekleyen siparişleri bulur
     */
    @Query("SELECT so FROM StockOrder so WHERE so.status IN ('PENDING', 'CONFIRMED', 'SHIPPED')")
    List<StockOrder> findPendingOrders();

    /**
     * Teslim tarihi geçen siparişleri bulur
     */
    @Query("SELECT so FROM StockOrder so WHERE so.expectedDeliveryDate < :currentDate AND so.status NOT IN ('DELIVERED', 'CANCELLED')")
    List<StockOrder> findOverdueOrders(@Param("currentDate") LocalDateTime currentDate);

    /**
     * Oluşturan kişiye göre siparişleri bulur
     */
    List<StockOrder> findByCreatedByOrderByOrderDateDesc(String createdBy);

    /**
     * Sipariş numarası var mı kontrolü
     */
    boolean existsByOrderNumber(String orderNumber);
}

