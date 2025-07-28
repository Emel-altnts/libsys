package com.d_tech.libsys.controller;

import com.d_tech.libsys.domain.model.StockOrder;
import com.d_tech.libsys.dto.StockOrderRequest;
import com.d_tech.libsys.dto.StockReceiptItem;
import com.d_tech.libsys.service.StockOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 🚀 UPDATED: Stok sipariş yönetim controller'ı - SHIPPED endpoint ve JSON fix eklendi
 */
@RestController
@RequestMapping("/api/stock/orders")
@RequiredArgsConstructor
@Slf4j
public class StockOrderController {

    private final StockOrderService stockOrderService;

    /**
     * Asenkron sipariş oluştur
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsyncResponse> createOrderAsync(
            @RequestBody StockOrderRequest orderRequest,
            Authentication authentication) {

        log.info("Asenkron sipariş oluşturma isteği: supplier={}, itemCount={}, user={}",
                orderRequest.getSupplierName(), orderRequest.getItems().size(), authentication.getName());

        // Oluşturan kişiyi set et
        orderRequest.setCreatedBy(authentication.getName());

        try {
            CompletableFuture<String> future = stockOrderService.createOrderAsync(orderRequest);
            String eventId = future.get();

            return ResponseEntity.accepted().body(new AsyncResponse(
                    "Sipariş oluşturma işlemi başlatıldı", eventId
            ));

        } catch (Exception e) {
            log.error("Sipariş oluşturma hatası: error={}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new AsyncResponse("Sipariş oluşturma işlemi başlatılamadı", null));
        }
    }

    /**
     * Sipariş onaylama
     */
    @PostMapping("/{orderId}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsyncResponse> confirmOrderAsync(
            @PathVariable Long orderId,
            Authentication authentication) {

        log.info("Sipariş onaylama isteği: orderId={}, user={}", orderId, authentication.getName());

        try {
            CompletableFuture<String> future = stockOrderService.confirmOrderAsync(orderId, authentication.getName());
            String eventId = future.get();

            return ResponseEntity.accepted().body(new AsyncResponse(
                    "Sipariş onaylama işlemi başlatıldı", eventId
            ));

        } catch (Exception e) {
            log.error("Sipariş onaylama hatası: orderId={}, error={}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new AsyncResponse("Sipariş onaylama işlemi başlatılamadı", null));
        }
    }

    /**
     * 🚀 YENİ: Sipariş kargoya verme (CONFIRMED → SHIPPED)
     */
    @PostMapping("/{orderId}/ship")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsyncResponse> shipOrderAsync(
            @PathVariable Long orderId,
            Authentication authentication) {

        log.info("Sipariş kargoya verme isteği: orderId={}, user={}", orderId, authentication.getName());

        try {
            CompletableFuture<String> future = stockOrderService.shipOrderAsync(orderId, authentication.getName());
            String eventId = future.get();

            return ResponseEntity.accepted().body(new AsyncResponse(
                    "Sipariş kargoya verme işlemi başlatıldı", eventId
            ));

        } catch (Exception e) {
            log.error("Sipariş kargoya verme hatası: orderId={}, error={}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new AsyncResponse("Sipariş kargoya verme işlemi başlatılamadı", null));
        }
    }

    /**
     * 🚀 UPDATED: Sipariş teslimat alma - JSON format düzeltildi
     */
    @PostMapping("/{orderId}/receive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsyncResponse> receiveOrderAsync(
            @PathVariable Long orderId,
            @RequestBody List<ReceiptItemRequest> receiptItems, // ✅ Direkt List, wrapper yok
            Authentication authentication) {

        log.info("Sipariş teslimat alma isteği: orderId={}, itemCount={}, user={}",
                orderId, receiptItems.size(), authentication.getName());

        try {
            // ✅ Önce sipariş durumunu kontrol et
            Optional<StockOrder> orderOpt = stockOrderService.getOrderById(orderId);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            StockOrder order = orderOpt.get();
            // ✅ CONFIRMED veya SHIPPED durumunda teslimat alınabilir
            if (order.getStatus() != StockOrder.OrderStatus.CONFIRMED &&
                    order.getStatus() != StockOrder.OrderStatus.SHIPPED) {
                return ResponseEntity.badRequest().body(new AsyncResponse(
                        "Sipariş bu durumda teslimat alınamaz: " + order.getStatus(), null));
            }

            // ReceiptItemRequest'i StockReceiptItem'a çevir
            List<StockReceiptItem> stockReceiptItems = receiptItems.stream()
                    .map(item -> StockReceiptItem.builder()
                            .orderItemId(item.getOrderItemId())
                            .receivedQuantity(item.getReceivedQuantity())
                            .notes(item.getNotes())
                            .build())
                    .toList();

            CompletableFuture<String> future = stockOrderService.receiveOrderAsync(
                    orderId, stockReceiptItems, authentication.getName());
            String eventId = future.get();

            return ResponseEntity.accepted().body(new AsyncResponse(
                    "Sipariş teslimat alma işlemi başlatıldı", eventId
            ));

        } catch (Exception e) {
            log.error("Sipariş teslimat alma hatası: orderId={}, error={}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new AsyncResponse("Sipariş teslimat alma işlemi başlatılamadı", null));
        }
    }

    /**
     * 🚀 YENİ: Sipariş kalemlerini getir (OrderItems debug için)
     */
    @GetMapping("/{orderId}/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderItemDto>> getOrderItems(@PathVariable Long orderId) {
        log.info("Sipariş kalemleri istendi: orderId={}", orderId);

        try {
            List<OrderItemDto> items = stockOrderService.getOrderItemsForDelivery(orderId);
            if (items.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            log.error("Sipariş kalemleri getirme hatası: orderId={}, error={}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 🚀 FIXED: Sipariş detayını getir - Simplified JSON Response
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getOrder(@PathVariable Long orderId) {
        System.out.println("=== FIXED: Sipariş detayı istendi ===");
        System.out.println("🔍 Aranan Order ID: " + orderId);

        log.info("Sipariş detayı istendi: orderId={}", orderId);

        try {
            Optional<StockOrder> orderOpt = stockOrderService.getOrderById(orderId);

            if (orderOpt.isPresent()) {
                StockOrder order = orderOpt.get();
                System.out.println("✅ Sipariş bulundu: ID=" + order.getId() + ", OrderNumber=" + order.getOrderNumber());

                // 🚀 CRITICAL FIX: Simplified DTO Response to avoid JSON serialization issues
                OrderResponseDto response = OrderResponseDto.builder()
                        .id(order.getId())
                        .orderNumber(order.getOrderNumber())
                        .supplierName(order.getSupplierName())
                        .supplierContact(order.getSupplierContact())
                        .orderDate(order.getOrderDate())
                        .expectedDeliveryDate(order.getExpectedDeliveryDate())
                        .actualDeliveryDate(order.getActualDeliveryDate())
                        .status(order.getStatus().toString())
                        .totalAmount(order.getTotalAmount())
                        .totalVat(order.getTotalVat())
                        .grandTotal(order.getGrandTotal())
                        .notes(order.getNotes())
                        .createdBy(order.getCreatedBy())
                        .createdAt(order.getCreatedAt())
                        .updatedAt(order.getUpdatedAt())
                        // Order items count (lazy loading issue önleme)
                        .orderItemsCount(order.getOrderItems() != null ? order.getOrderItems().size() : 0)
                        .hasInvoice(order.getInvoice() != null)
                        .build();

                return ResponseEntity.ok(response);

            } else {
                System.out.println("❌ Sipariş bulunamadı: orderId=" + orderId);

                // DEBUG: Mevcut siparişleri listele
                try {
                    List<StockOrder> allOrders = stockOrderService.getAllOrdersForDebug();
                    System.out.println("📊 Mevcut sipariş sayısı: " + allOrders.size());
                    allOrders.stream().limit(5).forEach(o ->
                            System.out.println("   - ID: " + o.getId() + ", OrderNumber: " + o.getOrderNumber())
                    );
                } catch (Exception debugEx) {
                    System.out.println("⚠️ Debug sorgusu başarısız: " + debugEx.getMessage());
                }

                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            System.out.println("💥 Sipariş detayı getirilirken hata: " + e.getMessage());
            log.error("Sipariş detayı getirme hatası: orderId={}, error={}", orderId, e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Sipariş detayı getirilirken hata oluştu");
        }
    }

    /**
     * Sipariş numarasıyla getir
     */
    @GetMapping("/number/{orderNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockOrder> getOrderByNumber(@PathVariable String orderNumber) {
        log.info("Sipariş detayı istendi: orderNumber={}", orderNumber);

        Optional<StockOrder> order = stockOrderService.getOrderByNumber(orderNumber);
        return order.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Duruma göre siparişleri listele
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderSummaryDto>> getOrdersByStatus(@PathVariable String status) {
        System.out.println("🔍 Duruma göre siparişler istendi: status=" + status);
        log.info("Duruma göre siparişler istendi: status={}", status);

        try {
            StockOrder.OrderStatus orderStatus = StockOrder.OrderStatus.valueOf(status.toUpperCase());
            List<StockOrder> orders = stockOrderService.getOrdersByStatus(orderStatus);

            System.out.println("📊 Bulunan sipariş sayısı: " + orders.size());

            // 🚀 FIXED: Simplified DTO to avoid JSON serialization issues
            List<OrderSummaryDto> response = orders.stream()
                    .map(this::toOrderSummaryDto)
                    .toList();

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Geçersiz sipariş durumu: " + status);
            log.warn("Geçersiz sipariş durumu: {}", status);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.out.println("💥 Siparişler getirilirken hata: " + e.getMessage());
            log.error("Siparişler getirme hatası: status={}, error={}", status, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 🚀 FIXED: Bekleyen siparişleri listele - Simplified JSON Response
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderSummaryDto>> getPendingOrders() {
        System.out.println("🔍 Bekleyen siparişler istendi");
        log.info("Bekleyen siparişler istendi");

        try {
            List<StockOrder> orders = stockOrderService.getPendingOrders();

            System.out.println("📊 Bekleyen sipariş sayısı: " + orders.size());

            // 🚀 CRITICAL FIX: Convert to simplified DTOs to avoid JSON serialization issues
            List<OrderSummaryDto> response = orders.stream()
                    .map(this::toOrderSummaryDto)
                    .toList();

            // Debug: İlk siparişin detaylarını yazdır
            if (!response.isEmpty()) {
                OrderSummaryDto firstOrder = response.get(0);
                System.out.println("📦 İlk sipariş detayları:");
                System.out.println("   ├── ID: " + firstOrder.getId());
                System.out.println("   ├── Order Number: " + firstOrder.getOrderNumber());
                System.out.println("   ├── Supplier: " + firstOrder.getSupplierName());
                System.out.println("   ├── Status: " + firstOrder.getStatus());
                System.out.println("   └── Created By: " + firstOrder.getCreatedBy());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("💥 Bekleyen siparişler getirilirken hata: " + e.getMessage());
            log.error("Bekleyen siparişler getirme hatası: {}", e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 🚀 NEW: Helper method to convert StockOrder to OrderSummaryDto
     */
    private OrderSummaryDto toOrderSummaryDto(StockOrder order) {
        return OrderSummaryDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .supplierName(order.getSupplierName())
                .orderDate(order.getOrderDate())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .status(order.getStatus().toString())
                .grandTotal(order.getGrandTotal())
                .createdBy(order.getCreatedBy())
                .build();
    }

    /**
     * Vadesi geçen siparişleri listele
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderSummaryDto>> getOverdueOrders() {
        log.info("Vadesi geçen siparişler istendi");

        try {
            List<StockOrder> orders = stockOrderService.getOverdueOrders();
            List<OrderSummaryDto> response = orders.stream()
                    .map(this::toOrderSummaryDto)
                    .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Vadesi geçen siparişler getirme hatası: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Tedarikçiye göre siparişleri listele
     */
    @GetMapping("/supplier/{supplierName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderSummaryDto>> getOrdersBySupplier(@PathVariable String supplierName) {
        log.info("Tedarikçiye göre siparişler istendi: supplier={}", supplierName);

        try {
            List<StockOrder> orders = stockOrderService.getOrdersBySupplier(supplierName);
            List<OrderSummaryDto> response = orders.stream()
                    .map(this::toOrderSummaryDto)
                    .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Tedarikçi siparişleri getirme hatası: supplier={}, error={}", supplierName, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Kullanıcının siparişlerini listele
     */
    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderSummaryDto>> getMyOrders(Authentication authentication) {
        log.info("Kullanıcı siparişleri istendi: user={}", authentication.getName());

        try {
            List<StockOrder> orders = stockOrderService.getOrdersByUser(authentication.getName());
            List<OrderSummaryDto> response = orders.stream()
                    .map(this::toOrderSummaryDto)
                    .toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Kullanıcı siparişleri getirme hatası: user={}, error={}", authentication.getName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Sipariş iptal et
     */
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @PathVariable Long orderId,
            @RequestBody CancelOrderRequest request) {

        log.info("Sipariş iptal isteği: orderId={}, reason={}", orderId, request.getReason());

        try {
            StockOrder cancelledOrder = stockOrderService.cancelOrder(orderId, request.getReason());

            OrderResponseDto response = OrderResponseDto.builder()
                    .id(cancelledOrder.getId())
                    .orderNumber(cancelledOrder.getOrderNumber())
                    .status(cancelledOrder.getStatus().toString())
                    .notes(cancelledOrder.getNotes())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Sipariş iptal hatası: orderId={}, error={}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    // 🚀 DTO sınıfları - Güncellenmiş
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class AsyncResponse {
        private String message;
        private String eventId;
    }

    @lombok.Data
    public static class ReceiptItemRequest {
        private Long orderItemId;
        private Integer receivedQuantity;
        private String notes;
    }

    @lombok.Data
    public static class CancelOrderRequest {
        private String reason;
    }

    /**
     * 🚀 NEW: OrderItem DTO - Teslimat için
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class OrderItemDto {
        private Long id;
        private Long bookId;
        private String bookTitle;
        private String bookAuthor;
        private Integer quantity;
        private BigDecimal unitPrice;
        private Integer receivedQuantity;
        private String notes;
        private BigDecimal subTotal;
    }

    /**
     * 🚀 NEW: Simplified Order Summary DTO for list endpoints
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class OrderSummaryDto {
        private Long id;
        private String orderNumber;
        private String supplierName;
        private java.time.LocalDateTime orderDate;
        private java.time.LocalDateTime expectedDeliveryDate;
        private String status;
        private java.math.BigDecimal grandTotal;
        private String createdBy;
    }

    /**
     * 🚀 NEW: Complete Order Response DTO for detail endpoint
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class OrderResponseDto {
        private Long id;
        private String orderNumber;
        private String supplierName;
        private String supplierContact;
        private java.time.LocalDateTime orderDate;
        private java.time.LocalDateTime expectedDeliveryDate;
        private java.time.LocalDateTime actualDeliveryDate;
        private String status;
        private java.math.BigDecimal totalAmount;
        private java.math.BigDecimal totalVat;
        private java.math.BigDecimal grandTotal;
        private String notes;
        private String createdBy;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
        private Integer orderItemsCount;
        private Boolean hasInvoice;
    }
}