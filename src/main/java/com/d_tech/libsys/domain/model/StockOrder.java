package com.d_tech.libsys.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Stok sipariş bilgilerini tutan entity
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "stock_orders")
public class StockOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @Column(name = "supplier_name", nullable = false)
    private String supplierName;

    @Column(name = "supplier_contact")
    private String supplierContact;

    @Column(name = "order_date")
    @Builder.Default
    private LocalDateTime orderDate = LocalDateTime.now();

    @Column(name = "expected_delivery_date")
    private LocalDateTime expectedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDateTime actualDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "total_vat", precision = 12, scale = 2)
    private BigDecimal totalVat;

    @Column(name = "grand_total", precision = 12, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_by")
    private String createdBy;

    @OneToMany(mappedBy = "stockOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StockOrderItem> orderItems;

    @OneToOne(mappedBy = "stockOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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
}
