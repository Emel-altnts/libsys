package com.d_tech.libsys.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Stok sipariş kalemi bilgilerini tutan entity
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "stock_order_items")
public class StockOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_order_id", nullable = false)
    private StockOrder stockOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "vat_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal vatRate = new BigDecimal("18.00"); // %18 KDV

    @Column(name = "discount_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountRate = BigDecimal.ZERO;

    @Column(name = "sub_total", precision = 12, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "vat_amount", precision = 12, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "received_quantity")
    @Builder.Default
    private Integer receivedQuantity = 0;

    @Column(name = "notes")
    private String notes;

    /**
     * Kalem tutarlarını hesaplar
     */
    public void calculateAmounts() {
        if (quantity == null || unitPrice == null) {
            subTotal = BigDecimal.ZERO;
            vatAmount = BigDecimal.ZERO;
            totalAmount = BigDecimal.ZERO;
            return;
        }

        // Ara toplam = miktar * birim fiyat
        BigDecimal grossAmount = unitPrice.multiply(new BigDecimal(quantity));

        // İndirim varsa uygula
        if (discountRate != null && discountRate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountAmount = grossAmount.multiply(discountRate).divide(new BigDecimal("100"));
            subTotal = grossAmount.subtract(discountAmount);
        } else {
            subTotal = grossAmount;
        }

        // KDV hesapla
        if (vatRate != null && vatRate.compareTo(BigDecimal.ZERO) > 0) {
            vatAmount = subTotal.multiply(vatRate).divide(new BigDecimal("100"));
        } else {
            vatAmount = BigDecimal.ZERO;
        }

        // Toplam tutar
        totalAmount = subTotal.add(vatAmount);
    }

    /**
     * Tam teslimat alındı mı kontrolü
     */
    public boolean isFullyReceived() {
        return receivedQuantity != null && receivedQuantity.equals(quantity);
    }

    /**
     * Kısmi teslimat var mı kontrolü
     */
    public boolean isPartiallyReceived() {
        return receivedQuantity != null && receivedQuantity > 0 && receivedQuantity < quantity;
    }

    /**
     * Bekleyen miktar
     */
    public int getPendingQuantity() {
        if (receivedQuantity == null) {
            return quantity;
        }
        return Math.max(0, quantity - receivedQuantity);
    }

    @PrePersist
    @PreUpdate
    protected void calculateOnSave() {
        calculateAmounts();
    }
}
