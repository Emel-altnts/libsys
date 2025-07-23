package com.d_tech.libsys.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 🚀 FIXED: Stok sipariş bilgilerini tutan entity - JSON Serialization düzeltildi
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "stock_orders")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // Hibernate proxy sorunları için
public class StockOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false, length = 50)
    private String orderNumber;

    @Column(name = "supplier_name", nullable = false, length = 100)
    private String supplierName;

    @Column(name = "supplier_contact", length = 200)
    private String supplierContact;

    @Column(name = "order_date", nullable = false)
    @Builder.Default
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(name = "expected_delivery_date")
    private LocalDateTime expectedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDateTime actualDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "total_vat", precision = 12, scale = 2)
    private BigDecimal totalVat;

    @Column(name = "grand_total", precision = 12, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 🚀 CRITICAL FIX: JSON Serialization için relationshipler ignore edildi
    @OneToMany(mappedBy = "stockOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // JSON serialization'da ignore et - sonsuz döngü önleme
    private List<StockOrderItem> orderItems;

    @OneToOne(mappedBy = "stockOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // JSON serialization'da ignore et - sonsuz döngü önleme
    private Invoice invoice;

    /**
     * Sipariş durumu enum'u
     */
    public enum OrderStatus {
        PENDING,        // Beklemede
        CONFIRMED,      // Onaylandı
        SHIPPED,        // Kargoya verildi
        DELIVERED,      // Teslim edildi
        CANCELLED,      // İptal edildi
        PARTIAL_DELIVERY // Kısmi teslimat
    }

    /**
     * Sipariş tutarlarını hesaplar
     */
    public void calculateTotals() {
        if (orderItems == null || orderItems.isEmpty()) {
            totalAmount = BigDecimal.ZERO;
            totalVat = BigDecimal.ZERO;
            grandTotal = BigDecimal.ZERO;
            return;
        }

        totalAmount = orderItems.stream()
                .map(StockOrderItem::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalVat = orderItems.stream()
                .map(StockOrderItem::getVatAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        grandTotal = totalAmount.add(totalVat);
    }

    /**
     * Sipariş tamamlandı mı kontrolü
     */
    public boolean isCompleted() {
        return status == OrderStatus.DELIVERED;
    }

    /**
     * Sipariş iptal edilebilir mi kontrolü
     */
    public boolean isCancellable() {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }

    /**
     * 🚀 NEW: Order items count (lazy loading olmadan)
     */
    @JsonIgnore
    public int getOrderItemsCount() {
        return orderItems != null ? orderItems.size() : 0;
    }

    /**
     * 🚀 NEW: Has invoice check (lazy loading olmadan)
     */
    @JsonIgnore
    public boolean hasInvoice() {
        return invoice != null;
    }

    /**
     * 🚀 NEW: Safe order items access (lazy loading kontrolü ile)
     */
    @JsonIgnore
    public List<StockOrderItem> getOrderItemsSafe() {
        try {
            return orderItems;
        } catch (org.hibernate.LazyInitializationException e) {
            // Lazy loading exception durumunda empty list döndür
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 🚀 NEW: Safe invoice access (lazy loading kontrolü ile)
     */
    @JsonIgnore
    public Invoice getInvoiceSafe() {
        try {
            return invoice;
        } catch (org.hibernate.LazyInitializationException e) {
            // Lazy loading exception durumunda null döndür
            return null;
        }
    }

    /**
     * Entity lifecycle metodları
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * toString metodu - JSON serialization sorunları önleme
     */
    @Override
    public String toString() {
        return "StockOrder{" +
                "id=" + id +
                ", orderNumber='" + orderNumber + '\'' +
                ", supplierName='" + supplierName + '\'' +
                ", status=" + status +
                ", createdBy='" + createdBy + '\'' +
                ", orderDate=" + orderDate +
                ", grandTotal=" + grandTotal +
                '}';
    }

    /**
     * equals ve hashCode (ID based) - JSON serialization için gerekli
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockOrder)) return false;
        StockOrder that = (StockOrder) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}