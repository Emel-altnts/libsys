package com.d_tech.libsys.controller;

import com.d_tech.libsys.domain.model.StockOrder;
import com.d_tech.libsys.repository.StockOrderRepository;
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
 * DEBUG Controller - Geliştirme aşamasında problemleri tespit etmek için
 */
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Slf4j
public class DebugController {

    private final StockOrderService stockOrderService;
    private final StockOrderRepository stockOrderRepository;

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
     * Transaction durumu testi
     */
    @GetMapping("/transaction/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testTransaction() {
        System.out.println("=== DEBUG: Transaction testi ===");

        Map<String, Object> result = new HashMap<>();

        try {
            // Transaction içinde okuma
            List<StockOrder> orders = stockOrderService.getAllOrdersForDebug();
            result.put("transactionReadOk", true);
            result.put("ordersRead", orders.size());

            System.out.println("✅ Transaction read OK - Okunan sipariş: " + orders.size());

        } catch (Exception e) {
            System.out.println("💥 Transaction test hatası: " + e.getMessage());
            result.put("transactionReadOk", false);
            result.put("error", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Hibernate session testi
     */
    @GetMapping("/hibernate/session")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testHibernateSession() {
        System.out.println("=== DEBUG: Hibernate session testi ===");

        Map<String, Object> result = new HashMap<>();

        try {
            // Hibernate specific sorgu
            List<StockOrder> orders = stockOrderRepository.findAll();
            result.put("hibernateQueryOk", true);
            result.put("ordersFound", orders.size());

            if (!orders.isEmpty()) {
                StockOrder firstOrder = orders.get(0);
                result.put("firstOrderId", firstOrder.getId());
                result.put("firstOrderIdType", firstOrder.getId().getClass().getSimpleName());

                // Aynı ID ile tekrar arama
                Optional<StockOrder> refound = stockOrderRepository.findById(firstOrder.getId());
                result.put("refoundSameOrder", refound.isPresent());
            }

            System.out.println("✅ Hibernate session OK");

        } catch (Exception e) {
            System.out.println("💥 Hibernate session hatası: " + e.getMessage());
            result.put("hibernateQueryOk", false);
            result.put("error", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }
}