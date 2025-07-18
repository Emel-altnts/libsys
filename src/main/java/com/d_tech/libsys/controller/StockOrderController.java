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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Stok sipariş yönetim controller'ı
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
     * Sipariş teslimat alma
     */
    @PostMapping("/{orderId}/receive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AsyncResponse> receiveOrderAsync(
            @PathVariable Long orderId,
            @RequestBody List<ReceiptItemRequest> receiptItems,
            Authentication authentication) {

        log.info("Sipariş teslimat alma isteği: orderId={}, itemCount={}, user={}",
                orderId, receiptItems.size(), authentication.getName());

        try {
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
     * Sipariş detayını getir
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockOrder> getOrder(@PathVariable Long orderId) {
        log.info("Sipariş detayı istendi: orderId={}", orderId);

        Optional<StockOrder> order = stockOrderService.getOrderById(orderId);
        return order.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
    public ResponseEntity<List<StockOrder>> getOrdersByStatus(@PathVariable String status) {
        log.info("Duruma göre siparişler istendi: status={}", status);

        try {
            StockOrder.OrderStatus orderStatus = StockOrder.OrderStatus.valueOf(status.toUpperCase());
            List<StockOrder> orders = stockOrderService.getOrdersByStatus(orderStatus);
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException e) {
            log.warn("Geçersiz sipariş durumu: {}", status);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Bekleyen siparişleri listele
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StockOrder>> getPendingOrders() {
        log.info("Bekleyen siparişler istendi");

        List<StockOrder> orders = stockOrderService.getPendingOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Vadesi geçen siparişleri listele
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StockOrder>> getOverdueOrders() {
        log.info("Vadesi geçen siparişler istendi");

        List<StockOrder> orders = stockOrderService.getOverdueOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Tedarikçiye göre siparişleri listele
     */
    @GetMapping("/supplier/{supplierName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StockOrder>> getOrdersBySupplier(@PathVariable String supplierName) {
        log.info("Tedarikçiye göre siparişler istendi: supplier={}", supplierName);

        List<StockOrder> orders = stockOrderService.getOrdersBySupplier(supplierName);
        return ResponseEntity.ok(orders);
    }

    /**
     * Kullanıcının siparişlerini listele
     */
    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StockOrder>> getMyOrders(Authentication authentication) {
        log.info("Kullanıcı siparişleri istendi: user={}", authentication.getName());

        List<StockOrder> orders = stockOrderService.getOrdersByUser(authentication.getName());
        return ResponseEntity.ok(orders);
    }

    /**
     * Sipariş iptal et
     */
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockOrder> cancelOrder(
            @PathVariable Long orderId,
            @RequestBody CancelOrderRequest request) {

        log.info("Sipariş iptal isteği: orderId={}, reason={}", orderId, request.getReason());

        try {
            StockOrder cancelledOrder = stockOrderService.cancelOrder(orderId, request.getReason());
            return ResponseEntity.ok(cancelledOrder);
        } catch (Exception e) {
            log.error("Sipariş iptal hatası: orderId={}, error={}", orderId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    // DTO sınıfları
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
}
