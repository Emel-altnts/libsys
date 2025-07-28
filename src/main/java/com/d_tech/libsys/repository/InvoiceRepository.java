package com.d_tech.libsys.repository;

import com.d_tech.libsys.domain.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ✅ SIMPLIFIED: Invoice repository - Sadece kullanılan metodlar
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Fatura numarasına göre bulur
     */
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * ✅ MAIN METHOD: Stok siparişine göre fatura bulur - Basit versiyon
     */
    Optional<Invoice> findByStockOrderId(Long stockOrderId);

    /**
     * Ödeme durumuna göre faturaları bulur
     */
    List<Invoice> findByPaymentStatus(Invoice.PaymentStatus paymentStatus);

    /**
     * Tedarikçiye göre faturaları bulur
     */
    List<Invoice> findBySupplierNameContainingIgnoreCase(String supplierName);

    /**
     * Tarih aralığında oluşturulan faturaları bulur
     */
    @Query("SELECT i FROM Invoice i WHERE i.invoiceDate BETWEEN :startDate AND :endDate")
    List<Invoice> findByInvoiceDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Vadesi geçen ödenmemiş faturaları bulur
     */
    @Query("SELECT i FROM Invoice i WHERE i.dueDate < :currentDate AND i.paymentStatus IN ('UNPAID', 'PARTIAL_PAID')")
    List<Invoice> findOverdueInvoices(@Param("currentDate") LocalDateTime currentDate);

    /**
     * Oluşturan kişiye göre faturaları bulur
     */
    List<Invoice> findByCreatedByOrderByInvoiceDateDesc(String createdBy);

    /**
     * Toplam ödenmemiş tutar hesaplar
     */
    @Query("SELECT COALESCE(SUM(i.grandTotal), 0) FROM Invoice i WHERE i.paymentStatus IN ('UNPAID', 'PARTIAL_PAID')")
    Double calculateTotalUnpaidAmount();

    /**
     * Belirli dönemdeki toplam fatura tutarı
     */
    @Query("SELECT COALESCE(SUM(i.grandTotal), 0) FROM Invoice i WHERE i.invoiceDate BETWEEN :startDate AND :endDate")
    Double calculateTotalInvoiceAmount(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Fatura numarası var mı kontrolü
     */
    boolean existsByInvoiceNumber(String invoiceNumber);

    @Query("SELECT i FROM Invoice i WHERE i.id = :id")
    Optional<Invoice> getByInvoiceId(@Param("id") Long id);



}