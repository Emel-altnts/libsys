package com.d_tech.libsys.controller;

import com.d_tech.libsys.domain.model.StockOrder;
import com.d_tech.libsys.domain.model.Invoice; // ✅ ADDED
import com.d_tech.libsys.repository.StockOrderRepository;
import com.d_tech.libsys.repository.InvoiceRepository; // ✅ ADDED
import com.d_tech.libsys.service.StockOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🚀 FIXED: DEBUG Controller - InvoiceRepository import eklendi
 */
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Slf4j
public class DebugController {

    private final StockOrderService stockOrderService;
    private final StockOrderRepository stockOrderRepository;
    private final InvoiceRepository invoiceRepository; // ✅ ADDED

    /**
     * Tüm stok siparişlerinin ID'lerini listele
     */
    @GetMapping("/stock-orders/ids")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllStockOrderIds() {
        System.out.println("=== DEBUG: Tüm sipariş ID'leri istendi ===");

        try {
            Map<String, Object> debug = new HashMap<>();

            // Tüm siparişleri getir
            List<StockOrder> allOrders = stockOrderRepository.findAll();
            debug.put("totalCount", allOrders.size());

            // ID'leri listele
            List<Long> ids = allOrders.stream().map(StockOrder::getId).toList();
            debug.put("allIds", ids);

            // Detaylı bilgi
            List<Map<String, Object>> orderDetails = allOrders.stream().map(order -> {
                Map<String, Object> detail = new HashMap<>();
                detail.put("id", order.getId());
                detail.put("orderNumber", order.getOrderNumber());
                detail.put("supplier", order.getSupplierName());
                detail.put("status", order.getStatus());
                detail.put("createdBy", order.getCreatedBy());
                return detail;
            }).toList();

            debug.put("orderDetails", orderDetails);

            System.out.println("📊 Toplam sipariş sayısı: " + allOrders.size());
            allOrders.forEach(o -> System.out.println("   - ID: " + o.getId() + ", Number: " + o.getOrderNumber()));

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            System.out.println("💥 Debug endpoint hatası: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("type", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Belirli ID ile sipariş arama testi
     */
    @GetMapping("/stock-orders/{orderId}/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testOrderSearch(@PathVariable Long orderId) {
        System.out.println("=== DEBUG: Sipariş arama testi ===");
        System.out.println("🔍 Test ID: " + orderId);

        Map<String, Object> result = new HashMap<>();
        result.put("searchedId", orderId);
        result.put("idType", orderId.getClass().getSimpleName());

        try {
            // 1. Repository direkt kullanım
            System.out.println("🔍 1. Repository.findById test...");
            Optional<StockOrder> repoResult = stockOrderRepository.findById(orderId);
            result.put("repositoryFound", repoResult.isPresent());
            if (repoResult.isPresent()) {
                result.put("repositoryOrder", Map.of(
                        "id", repoResult.get().getId(),
                        "orderNumber", repoResult.get().getOrderNumber(),
                        "supplier", repoResult.get().getSupplierName()
                ));
            }

            // 2. Service kullanım
            System.out.println("🔍 2. Service.getOrderById test...");
            Optional<StockOrder> serviceResult = stockOrderService.getOrderById(orderId);
            result.put("serviceFound", serviceResult.isPresent());
            if (serviceResult.isPresent()) {
                result.put("serviceOrder", Map.of(
                        "id", serviceResult.get().getId(),
                        "orderNumber", serviceResult.get().getOrderNumber(),
                        "supplier", serviceResult.get().getSupplierName()
                ));
            }

            // 3. Debug repository metodları
            System.out.println("🔍 3. Debug repository metodları test...");
            boolean debugExists = stockOrderRepository.debugExistsById(orderId);
            result.put("debugExists", debugExists);

            Optional<StockOrder> debugNative = stockOrderRepository.debugFindByIdNative(orderId);
            result.put("debugNativeFound", debugNative.isPresent());

            // 4. Tüm ID'leri kontrol et
            List<Long> allIds = stockOrderRepository.debugGetAllIds();
            result.put("allDatabaseIds", allIds);
            result.put("idExistsInList", allIds.contains(orderId));

            // 5. ID ve Order Number eşleştirmesi
            List<Object[]> idOrderPairs = stockOrderRepository.debugGetIdAndOrderNumber();
            result.put("idOrderPairs", idOrderPairs.stream().map(pair ->
                    Map.of("id", pair[0], "orderNumber", pair[1])
            ).toList());

            System.out.println("✅ Test sonuçları hazırlandı");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.out.println("💥 Test hatası: " + e.getMessage());
            e.printStackTrace();

            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 🚀 NEW: Invoice-Order relationship debug
     */
    @GetMapping("/invoices/{orderId}/debug")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> debugInvoiceOrderRelation(@PathVariable Long orderId) {
        System.out.println("=== DEBUG: Invoice-Order İlişkisi ===");
        System.out.println("🔍 Order ID: " + orderId);

        Map<String, Object> debug = new HashMap<>();
        debug.put("orderId", orderId);

        try {
            // 1. Order var mı?
            Optional<StockOrder> orderOpt = stockOrderService.getOrderById(orderId);
            debug.put("orderExists", orderOpt.isPresent());

            if (orderOpt.isPresent()) {
                StockOrder order = orderOpt.get();
                debug.put("orderNumber", order.getOrderNumber());
                debug.put("orderStatus", order.getStatus().toString());
                debug.put("orderTotal", order.getGrandTotal());

                // 2. Order'ın faturası var mı? (Lazy loading test)
                try {
                    // ⚠️ Dikkat: Bu lazy loading exception verebilir
                    Invoice orderInvoice = order.getInvoice();
                    debug.put("orderHasInvoice", orderInvoice != null);
                    if (orderInvoice != null) {
                        debug.put("invoiceFromOrder", Map.of(
                                "id", orderInvoice.getId(),
                                "number", orderInvoice.getInvoiceNumber(),
                                "total", orderInvoice.getGrandTotal()
                        ));
                    }
                } catch (Exception e) {
                    debug.put("orderInvoiceError", e.getMessage());
                    debug.put("lazyLoadingFailed", true);
                }
            }

            // 3. Repository'den direkt fatura arama
            List<Invoice> allInvoices = invoiceRepository.findAll();
            debug.put("totalInvoicesInDB", allInvoices.size());

            // OrderId ile eşleşen faturaları bul
            List<Invoice> matchingInvoices = allInvoices.stream()
                    .filter(inv -> {
                        try {
                            return inv.getStockOrder() != null && inv.getStockOrder().getId().equals(orderId);
                        } catch (Exception e) {
                            return false; // Lazy loading exception durumunda false döndür
                        }
                    })
                    .toList();

            debug.put("matchingInvoicesCount", matchingInvoices.size());
            debug.put("matchingInvoices", matchingInvoices.stream().map(inv -> {
                try {
                    return Map.of(
                            "id", inv.getId(),
                            "number", inv.getInvoiceNumber(),
                            "orderId", inv.getStockOrder().getId()
                    );
                } catch (Exception e) {
                    return Map.of(
                            "id", inv.getId(),
                            "number", inv.getInvoiceNumber(),
                            "orderIdError", e.getMessage()
                    );
                }
            }).toList());

            // 4. Repository metodu test
            Optional<Invoice> repoResult = invoiceRepository.findByStockOrderId(orderId);
            debug.put("repositoryFindsInvoice", repoResult.isPresent());
            if (repoResult.isPresent()) {
                debug.put("repositoryInvoice", Map.of(
                        "id", repoResult.get().getId(),
                        "number", repoResult.get().getInvoiceNumber()
                ));
            }

            // 5. Fatura listesi - detaylı
            debug.put("allInvoiceDetails", allInvoices.stream().limit(10).map(inv -> {
                Map<String, Object> invDetail = new HashMap<>();
                invDetail.put("id", inv.getId());
                invDetail.put("number", inv.getInvoiceNumber());
                invDetail.put("supplierName", inv.getSupplierName());
                invDetail.put("grandTotal", inv.getGrandTotal());

                try {
                    if (inv.getStockOrder() != null) {
                        invDetail.put("stockOrderId", inv.getStockOrder().getId());
                    } else {
                        invDetail.put("stockOrderId", null);
                    }
                } catch (Exception e) {
                    invDetail.put("stockOrderError", e.getMessage());
                }

                return invDetail;
            }).toList());

            System.out.println("✅ Debug analizi tamamlandı");
            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            System.out.println("💥 Debug hatası: " + e.getMessage());
            e.printStackTrace();
            debug.put("error", e.getMessage());
            debug.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(debug);
        }
    }

    /**
     * Veritabanı bağlantı testi
     */
    @GetMapping("/database/connection")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testDatabaseConnection() {
        System.out.println("=== DEBUG: Veritabanı bağlantı testi ===");

        Map<String, Object> result = new HashMap<>();

        try {
            // Basit count sorgusu
            long count = stockOrderRepository.count();
            result.put("connectionOk", true);
            result.put("totalOrders", count);

            // Tablo yapısı bilgisi
            List<Long> ids = stockOrderRepository.debugGetAllIds();
            result.put("canQueryIds", true);
            result.put("firstFewIds", ids.stream().limit(5).toList());

            System.out.println("✅ Veritabanı bağlantısı OK - Toplam sipariş: " + count);

        } catch (Exception e) {
            System.out.println("💥 Veritabanı bağlantı hatası: " + e.getMessage());
            result.put("connectionOk", false);
            result.put("error", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * JSON Serialization Test
     */
    @GetMapping("/json/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testJsonSerialization() {
        System.out.println("=== JSON Serialization Test ===");

        Map<String, Object> result = new HashMap<>();

        try {
            // Tek bir sipariş getir
            List<StockOrder> allOrders = stockOrderRepository.findAll();
            result.put("totalOrders", allOrders.size());

            if (!allOrders.isEmpty()) {
                StockOrder firstOrder = allOrders.get(0);

                // Temel bilgileri test et
                Map<String, Object> orderInfo = new HashMap<>();
                orderInfo.put("id", firstOrder.getId());
                orderInfo.put("orderNumber", firstOrder.getOrderNumber());
                orderInfo.put("supplierName", firstOrder.getSupplierName());
                orderInfo.put("status", firstOrder.getStatus().toString());
                orderInfo.put("createdBy", firstOrder.getCreatedBy());
                orderInfo.put("orderDate", firstOrder.getOrderDate().toString());

                // Lazy loading test
                try {
                    int itemCount = firstOrder.getOrderItems() != null ? firstOrder.getOrderItems().size() : 0;
                    orderInfo.put("orderItemsCount", itemCount);
                    orderInfo.put("lazyLoadingWorks", true);
                } catch (Exception e) {
                    orderInfo.put("orderItemsCount", "LAZY_LOADING_ERROR");
                    orderInfo.put("lazyLoadingWorks", false);
                    orderInfo.put("lazyError", e.getMessage());
                }

                result.put("firstOrder", orderInfo);
                result.put("success", true);
            } else {
                result.put("message", "No orders found");
                result.put("success", false);
            }

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("success", false);
            e.printStackTrace();
        }

        return ResponseEntity.ok(result);
    }
}