package com.d_tech.libsys.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fatura bilgilerini tutan entity
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", unique = true, nullable = false)
    private String invoiceNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_order_id")
    private StockOrder stockOrder;

    @Column(name = "invoice_date")
    @Builder.Default
    private LocalDateTime invoiceDate = LocalDateTime.now();

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "supplier_name", nullable = false)
    private String supplierName;

    @Column(name = "supplier_address")
    private String supplierAddress;

    @Column(name = "supplier_tax_number")
    private String supplierTaxNumber;

    @Column(name = "supplier_phone")
    private String supplierPhone;

    @Column(name = "supplier_email")
    private String supplierEmail;

    @Column(name = "buyer_name")
    private String buyerName;

    @Column(name = "buyer_address")
    private String buyerAddress;

    @Column(name = "buyer_tax_number")
    private String buyerTaxNumber;

    @Column(name = "sub_total", precision = 12, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "total_vat", precision = 12, scale = 2)
    private BigDecimal totalVat;

    @Column(name = "total_discount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    @Column(name = "grand_total", precision = 12, scale = 2)
    private BigDecimal grandTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Ödeme durumu enum'u
     */
    public enum PaymentStatus {
        UNPAID,        // Ödenmedi
        PARTIAL_PAID,  // Kısmi ödendi
        PAID,          // Ödendi
        OVERDUE,       // Vadesi geçti
        CANCELLED      // İptal edildi
    }

    /**
     * Fatura tutarlarını stok siparişinden kopyalar
     */
    public void copyAmountsFromOrder() {
        if (stockOrder != null) {
            this.subTotal = stockOrder.getTotalAmount();
            this.totalVat = stockOrder.getTotalVat();
            this.grandTotal = stockOrder.getGrandTotal();
        }
    }

    /**
     * Fatura vadesi geçti mi kontrolü
     */
    public boolean isOverdue() {
        return dueDate != null &&
                LocalDateTime.now().isAfter(dueDate) &&
                paymentStatus != PaymentStatus.PAID;
    }

    /**
     * Fatura ödendi olarak işaretle
     */
    public void markAsPaid(String paymentMethod) {
        this.paymentStatus = PaymentStatus.PAID;
        this.paymentDate = LocalDateTime.now();
        this.paymentMethod = paymentMethod;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Fatura numarası oluştur
     */
    public static String generateInvoiceNumber() {
        return "INV-" + System.currentTimeMillis();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        // Vade kontrolü
        if (isOverdue() && paymentStatus == PaymentStatus.UNPAID) {
            paymentStatus = PaymentStatus.OVERDUE;
        }
    }
}