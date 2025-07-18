package com.d_tech.libsys.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kitap stok bilgilerini tutan entity
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "book_stocks")
public class BookStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "current_quantity", nullable = false)
    private Integer currentQuantity;

    @Column(name = "minimum_quantity")
    @Builder.Default
    private Integer minimumQuantity = 10;

    @Column(name = "maximum_quantity")
    @Builder.Default
    private Integer maximumQuantity = 1000;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "supplier_contact")
    private String supplierContact;

    @Column(name = "last_order_date")
    private LocalDateTime lastOrderDate;

    @Column(name = "last_order_quantity")
    private Integer lastOrderQuantity;

    @Column(name = "vat_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal vatRate = new BigDecimal("18.00"); // %18 KDV

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private StockStatus status = StockStatus.SUFFICIENT;

    /**
     * Stok durumu enum'u
     */
    public enum StockStatus {
        SUFFICIENT,    // Yeterli stok
        LOW_STOCK,     // Düşük stok
        OUT_OF_STOCK,  // Stok tükendi
        OVERSTOCK      // Fazla stok
    }

    /**
     * Stok durumunu hesaplar ve günceller
     */
    public void updateStockStatus() {
        if (currentQuantity == 0) {
            this.status = StockStatus.OUT_OF_STOCK;
        } else if (currentQuantity <= minimumQuantity) {
            this.status = StockStatus.LOW_STOCK;
        } else if (currentQuantity >= maximumQuantity) {
            this.status = StockStatus.OVERSTOCK;
        } else {
            this.status = StockStatus.SUFFICIENT;
        }
    }

    /**
     * Stok azaltma
     */
    public boolean decreaseStock(int quantity) {
        if (currentQuantity >= quantity) {
            currentQuantity -= quantity;
            updateStockStatus();
            updatedAt = LocalDateTime.now();
            return true;
        }
        return false;
    }

    /**
     * Stok artırma
     */
    public void increaseStock(int quantity) {
        currentQuantity += quantity;
        updateStockStatus();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Stok gerekli mi kontrolü
     */
    public boolean isRestockNeeded() {
        return status == StockStatus.OUT_OF_STOCK || status == StockStatus.LOW_STOCK;
    }

    /**
     * Önerilen sipariş miktarı
     */
    public int getRecommendedOrderQuantity() {
        if (isRestockNeeded()) {
            return maximumQuantity - currentQuantity;
        }
        return 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        updateStockStatus();
    }
}
