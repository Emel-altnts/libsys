package com.d_tech.libsys.service;

import com.d_tech.libsys.domain.model.*;
import com.d_tech.libsys.dto.*;
import com.d_tech.libsys.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Stok sipariş yönetim servisi - FIXED VERSION
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StockOrderService {

    private final StockOrderRepository stockOrderRepository;
    private final StockOrderItemRepository stockOrderItemRepository;
    private final BookRepository bookRepository;
    private final BookStockRepository bookStockRepository;
    private final KafkaProducerService kafkaProducerService;
    private final EntityManager entityManager; // ✅ EntityManager injection eklendi

    /**
     * Asenkron sipariş oluşturma
     */
    public CompletableFuture<String> createOrderAsync(StockOrderRequest orderRequest) {
        log.info("Asenkron sipariş oluşturma başlatılıyor: supplier={}, itemCount={}",
                orderRequest.getSupplierName(), orderRequest.getItems().size());

        try {
            // Temel validasyonlar
            validateOrderRequest(orderRequest);

            // Event oluştur
            StockOrderEvent event = StockOrderEvent.builder()
                    .eventId(generateEventId("CREATE_ORDER"))
                    .eventType(StockOrderEvent.EventType.CREATE_ORDER)
                    .orderRequest(orderRequest)
                    .build();

            // Kafka'ya gönder
            return kafkaProducerService.sendStockOrderEvent(event)
                    .thenApply(success -> {
                        if (success) {
                            log.info("Sipariş oluşturma event'i gönderildi: eventId={}", event.getEventId());
                            return event.getEventId();
                        } else {
                            log.error("Sipariş oluşturma event'i gönderilemedi");
                            throw new RuntimeException("Sipariş oluşturma event'i gönderilemedi");
                        }
                    });

        } catch (Exception e) {
            log.error("Asenkron sipariş oluşturma hatası: error={}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Sipariş onaylama
     */
    public CompletableFuture<String> confirmOrderAsync(Long orderId, String userId) {
        log.info("Sipariş onaylama başlatılıyor: orderId={}, userId={}", orderId, userId);

        try {
            StockOrderEvent event = StockOrderEvent.builder()
                    .eventId(generateEventId("CONFIRM_ORDER"))
                    .eventType(StockOrderEvent.EventType.CONFIRM_ORDER)
                    .orderId(orderId)
                    .build();

            return kafkaProducerService.sendStockOrderEvent(event)
                    .thenApply(success -> {
                        if (success) {
                            log.info("Sipariş onaylama event'i gönderildi: eventId={}", event.getEventId());
                            return event.getEventId();
                        } else {
                            throw new RuntimeException("Sipariş onaylama event'i gönderilemedi");
                        }
                    });

        } catch (Exception e) {
            log.error("Sipariş onaylama hatası: orderId={}, error={}", orderId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Sipariş teslimat alma
     */
    public CompletableFuture<String> receiveOrderAsync(Long orderId, List<StockReceiptItem> receiptItems, String userId) {
        log.info("Sipariş teslimat alma başlatılıyor: orderId={}, userId={}", orderId, userId);

        try {
            StockOrderEvent event = StockOrderEvent.builder()
                    .eventId(generateEventId("RECEIVE_ORDER"))
                    .eventType(StockOrderEvent.EventType.RECEIVE_ORDER)
                    .orderId(orderId)
                    .message("Receipt items: " + receiptItems.size())
                    .build();

            return kafkaProducerService.sendStockOrderEvent(event)
                    .thenApply(success -> {
                        if (success) {
                            log.info("Sipariş teslimat event'i gönderildi: eventId={}", event.getEventId());
                            return event.getEventId();
                        } else {
                            throw new RuntimeException("Sipariş teslimat event'i gönderilemedi");
                        }
                    });

        } catch (Exception e) {
            log.error("Sipariş teslimat hatası: orderId={}, error={}", orderId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 🚀 FIXED: Sipariş detayını getir - ENHANCED VERSION WITH MULTIPLE STRATEGIES
     */
    @Transactional(readOnly = true)
    public Optional<StockOrder> getOrderById(Long orderId) {
        System.out.println("=== StockOrderService.getOrderById FIXED VERSION ===");
        System.out.println("🔍 Aranan ID: " + orderId);
        System.out.println("🔍 ID Type: " + (orderId != null ? orderId.getClass().getSimpleName() : "null"));

        log.info("Sipariş detayı getiriliyor: orderId={}", orderId);

        try {
            // ✅ 1. Input validation
            if (orderId == null || orderId <= 0) {
                System.out.println("❌ Geçersiz orderId: " + orderId);
                log.warn("Geçersiz orderId: {}", orderId);
                return Optional.empty();
            }

            // ✅ 2. EntityManager flush - pending changes'i commit et
            System.out.println("🔍 EntityManager flush yapılıyor...");
            entityManager.flush();

            // ✅ 3. İlk arama - standart repository metodu
            System.out.println("🔍 Strategy 1: Repository.findById...");
            Optional<StockOrder> result = stockOrderRepository.findById(orderId);

            if (result.isPresent()) {
                StockOrder order = result.get();
                System.out.println("✅ Strategy 1 SUCCESS: ID=" + order.getId() + ", OrderNumber=" + order.getOrderNumber());
                log.info("Sipariş bulundu (Strategy 1): orderId={}, orderNumber={}", orderId, order.getOrderNumber());
                return result;
            }

            // ✅ 4. Strategy 2 - Cache clear ve tekrar dene
            System.out.println("🔍 Strategy 2: Cache clear + retry...");
            entityManager.clear();
            result = stockOrderRepository.findById(orderId);

            if (result.isPresent()) {
                StockOrder order = result.get();
                System.out.println("✅ Strategy 2 SUCCESS: Cache clear sonrası bulundu");
                log.info("Sipariş bulundu (Strategy 2 - Cache Clear): orderId={}, orderNumber={}", orderId, order.getOrderNumber());
                return result;
            }

            // ✅ 5. Strategy 3 - Native query
            System.out.println("🔍 Strategy 3: Native query...");
            try {
                Query nativeQuery = entityManager.createNativeQuery(
                        "SELECT * FROM stock_orders WHERE id = ?", StockOrder.class);
                nativeQuery.setParameter(1, orderId);

                StockOrder order = (StockOrder) nativeQuery.getSingleResult();
                System.out.println("✅ Strategy 3 SUCCESS: Native query ile bulundu - " + order.getOrderNumber());
                log.info("Sipariş bulundu (Strategy 3 - Native): orderId={}, orderNumber={}", orderId, order.getOrderNumber());
                return Optional.of(order);

            } catch (NoResultException e) {
                System.out.println("❌ Strategy 3 FAILED: Native query ile de bulunamadı");
            }

            // ✅ 6. Strategy 4 - Debug repository metodu
            System.out.println("🔍 Strategy 4: Debug repository metodu...");
            try {
                Optional<StockOrder> debugResult = stockOrderRepository.debugFindByIdNative(orderId);
                if (debugResult.isPresent()) {
                    StockOrder order = debugResult.get();
                    System.out.println("✅ Strategy 4 SUCCESS: Debug native query ile bulundu");
                    log.info("Sipariş bulundu (Strategy 4 - Debug): orderId={}, orderNumber={}", orderId, order.getOrderNumber());
                    return debugResult;
                }
            } catch (Exception debugEx) {
                System.out.println("⚠️ Strategy 4 ERROR: " + debugEx.getMessage());
            }

            // ✅ 7. Final check - existsById kontrolü
            System.out.println("🔍 Final Check: existsById...");
            boolean exists = stockOrderRepository.existsById(orderId);
            System.out.println("🔍 ExistsById sonucu: " + exists);

            if (exists) {
                System.out.println("⚠️ PARADOX: ID var ama hiçbir strategy ile bulunamadı!");
                log.error("PARADOX: Order exists but not found with any strategy: orderId={}", orderId);

                // Son çare - tüm siparişleri getir ve manuel ara
                List<StockOrder> allOrders = stockOrderRepository.findAll();
                Optional<StockOrder> manualFind = allOrders.stream()
                        .filter(o -> o.getId().equals(orderId))
                        .findFirst();

                if (manualFind.isPresent()) {
                    System.out.println("✅ MANUAL SEARCH SUCCESS!");
                    return manualFind;
                }
            }

            // ✅ 8. Son durum - bulunamadı
            System.out.println("❌ FINAL RESULT: Sipariş hiçbir yöntemle bulunamadı - orderId=" + orderId);
            log.warn("Sipariş hiçbir strategy ile bulunamadı: orderId={}", orderId);

            // Debug bilgisi yazdır
            printDebugInfo(orderId);

            return Optional.empty();

        } catch (Exception e) {
            System.out.println("💥 getOrderById kritik hatası: " + e.getMessage());
            log.error("Sipariş detayı getirme kritik hatası: orderId={}, error={}", orderId, e.getMessage(), e);
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * ✅ DEBUG: Detaylı debug bilgisi yazdır
     */
    private void printDebugInfo(Long searchId) {
        try {
            System.out.println("=== DEBUG INFO ===");

            // Toplam sipariş sayısı
            long totalCount = stockOrderRepository.count();
            System.out.println("📊 Toplam sipariş sayısı: " + totalCount);

            // Tüm ID'leri listele
            List<Long> allIds = stockOrderRepository.debugGetAllIds();
            System.out.println("📊 Tüm ID'ler (" + allIds.size() + "): " + allIds);

            // Search ID'nin listede var olup olmadığını kontrol et
            boolean inList = allIds.contains(searchId);
            System.out.println("🔍 Search ID listede var mı: " + inList);

            // ID range bilgisi
            if (!allIds.isEmpty()) {
                Long minId = allIds.stream().min(Long::compare).orElse(0L);
                Long maxId = allIds.stream().max(Long::compare).orElse(0L);
                System.out.println("📊 ID Aralığı: " + minId + " - " + maxId);
                System.out.println("🔍 Search ID aralık içinde mi: " + (searchId >= minId && searchId <= maxId));
            }

            // İlk 5 siparişin detayları
            List<StockOrder> sample = stockOrderRepository.findAll().stream().limit(5).toList();
            System.out.println("📊 İlk 5 sipariş:");
            sample.forEach(o -> System.out.println("   - ID: " + o.getId() +
                    " (type: " + o.getId().getClass().getSimpleName() +
                    "), OrderNumber: " + o.getOrderNumber() +
                    ", Supplier: " + o.getSupplierName()));

        } catch (Exception debugEx) {
            System.out.println("⚠️ Debug info yazdırılırken hata: " + debugEx.getMessage());
        }
    }

    /**
     * ✅ Cache clearing ile sipariş arama
     */
    @Transactional(readOnly = true)
    public Optional<StockOrder> getOrderByIdWithCacheClear(Long orderId) {
        System.out.println("=== Cache Clear Version ===");
        log.info("Cache clear ile sipariş aranıyor: orderId={}", orderId);

        try {
            // Cache'i temizle
            entityManager.clear();

            // Tekrar ara
            Optional<StockOrder> result = stockOrderRepository.findById(orderId);

            if (result.isPresent()) {
                System.out.println("✅ Cache clear sonrası bulundu: " + result.get().getOrderNumber());
                return result;
            }

            // Native query ile tekrar dene
            Query nativeQuery = entityManager.createNativeQuery(
                    "SELECT * FROM stock_orders WHERE id = ?", StockOrder.class);
            nativeQuery.setParameter(1, orderId);

            try {
                StockOrder order = (StockOrder) nativeQuery.getSingleResult();
                result = Optional.of(order);
                System.out.println("✅ Native query ile bulundu: " + order.getOrderNumber());
            } catch (NoResultException e) {
                System.out.println("❌ Native query ile de bulunamadı");
            }

            return result;

        } catch (Exception e) {
            log.error("Cache clear version hatası: orderId={}, error={}", orderId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * ✅ DEBUG: Tüm siparişleri getir (debug için)
     */
    @Transactional(readOnly = true)
    public List<StockOrder> getAllOrdersForDebug() {
        log.info("DEBUG: Tüm siparişler getiriliyor");

        try {
            List<StockOrder> allOrders = stockOrderRepository.findAll();
            log.info("DEBUG: Toplam {} sipariş bulundu", allOrders.size());

            allOrders.forEach(order -> {
                log.info("DEBUG: Order - ID: {}, Number: {}, Supplier: {}, Status: {}",
                        order.getId(), order.getOrderNumber(), order.getSupplierName(), order.getStatus());
            });

            return allOrders;
        } catch (Exception e) {
            log.error("DEBUG: Tüm siparişler getirme hatası: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Senkron sipariş oluşturma (Consumer tarafından çağrılır)
     */
    @Transactional
    public StockOrder createOrder(StockOrderRequest orderRequest) {
        log.info("Sipariş oluşturuluyor: supplier={}", orderRequest.getSupplierName());

        // Sipariş numarası oluştur
        String orderNumber = generateOrderNumber();

        // Sipariş oluştur
        StockOrder order = StockOrder.builder()
                .orderNumber(orderNumber)
                .supplierName(orderRequest.getSupplierName())
                .supplierContact(orderRequest.getSupplierContact())
                .expectedDeliveryDate(orderRequest.getExpectedDeliveryDate())
                .notes(orderRequest.getNotes())
                .createdBy(orderRequest.getCreatedBy())
                .build();

        // Sipariş kaydet
        StockOrder savedOrder = stockOrderRepository.save(order);

        // Sipariş kalemlerini oluştur
        for (StockOrderItemRequest itemRequest : orderRequest.getItems()) {
            StockOrderItem item = createOrderItem(savedOrder, itemRequest);
            stockOrderItemRepository.save(item);
        }

        // Tutarları hesapla
        savedOrder.calculateTotals();
        stockOrderRepository.save(savedOrder);

        log.info("Sipariş oluşturuldu: orderId={}, orderNumber={}, total={}",
                savedOrder.getId(), savedOrder.getOrderNumber(), savedOrder.getGrandTotal());

        return savedOrder;
    }

    /**
     * Sipariş kalemi oluştur
     */
    private StockOrderItem createOrderItem(StockOrder order, StockOrderItemRequest itemRequest) {
        // Kitap kontrol et
        Book book = bookRepository.findById(itemRequest.getBookId())
                .orElseThrow(() -> new IllegalArgumentException("Kitap bulunamadı: " + itemRequest.getBookId()));

        return StockOrderItem.builder()
                .stockOrder(order)
                .book(book)
                .quantity(itemRequest.getQuantity())
                .unitPrice(itemRequest.getUnitPrice())
                .vatRate(itemRequest.getVatRate() != null ? itemRequest.getVatRate() : new BigDecimal("18.00"))
                .discountRate(itemRequest.getDiscountRate() != null ? itemRequest.getDiscountRate() : BigDecimal.ZERO)
                .notes(itemRequest.getNotes())
                .build();
    }

    /**
     * Sipariş onaylama (Consumer tarafından çağrılır)
     */
    @Transactional
    public StockOrder confirmOrder(Long orderId) {
        log.info("Sipariş onaylanıyor: orderId={}", orderId);

        StockOrder order = stockOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı: " + orderId));

        if (!order.isCancellable()) {
            throw new IllegalStateException("Sipariş onaylanamaz durumda: " + order.getStatus());
        }

        order.setStatus(StockOrder.OrderStatus.CONFIRMED);
        StockOrder savedOrder = stockOrderRepository.save(order);

        log.info("Sipariş onaylandı: orderId={}, status={}", orderId, savedOrder.getStatus());
        return savedOrder;
    }

    /**
     * Sipariş teslimat alma (Consumer tarafından çağrılır)
     */
    @Transactional
    public StockOrder receiveOrder(Long orderId, List<StockReceiptItem> receiptItems) {
        log.info("Sipariş teslimatı alınıyor: orderId={}, itemCount={}", orderId, receiptItems.size());

        StockOrder order = stockOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı: " + orderId));

        List<StockOrderItem> orderItems = stockOrderItemRepository.findByStockOrderId(orderId);
        boolean fullyReceived = true;

        // Her sipariş kalemi için teslimat güncelle
        for (StockReceiptItem receiptItem : receiptItems) {
            StockOrderItem orderItem = orderItems.stream()
                    .filter(item -> item.getId().equals(receiptItem.getOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Sipariş kalemi bulunamadı: " + receiptItem.getOrderItemId()));

            // Teslimat miktarını güncelle
            orderItem.setReceivedQuantity(receiptItem.getReceivedQuantity());
            stockOrderItemRepository.save(orderItem);

            // Stoku güncelle
            updateBookStockAfterReceipt(orderItem, receiptItem.getReceivedQuantity());

            // Tam teslimat kontrolü
            if (!orderItem.isFullyReceived()) {
                fullyReceived = false;
            }
        }

        // Sipariş durumunu güncelle
        if (fullyReceived) {
            order.setStatus(StockOrder.OrderStatus.DELIVERED);
            order.setActualDeliveryDate(LocalDateTime.now());
        } else {
            order.setStatus(StockOrder.OrderStatus.PARTIAL_DELIVERY);
        }

        StockOrder savedOrder = stockOrderRepository.save(order);
        log.info("Sipariş teslimatı tamamlandı: orderId={}, status={}", orderId, savedOrder.getStatus());

        return savedOrder;
    }

    /**
     * Teslimat sonrası stok güncelleme
     */
    private void updateBookStockAfterReceipt(StockOrderItem orderItem, Integer receivedQuantity) {
        Optional<BookStock> stockOpt = bookStockRepository.findByBookId(orderItem.getBook().getId());

        if (stockOpt.isPresent()) {
            BookStock stock = stockOpt.get();
            stock.increaseStock(receivedQuantity);
            stock.setLastOrderDate(LocalDateTime.now());
            stock.setLastOrderQuantity(receivedQuantity);
            bookStockRepository.save(stock);

            log.info("Stok güncellendi: bookId={}, oldQty={}, receivedQty={}, newQty={}",
                    orderItem.getBook().getId(),
                    stock.getCurrentQuantity() - receivedQuantity,
                    receivedQuantity,
                    stock.getCurrentQuantity());
        } else {
            log.warn("Stok kaydı bulunamadı: bookId={}", orderItem.getBook().getId());
        }
    }

    /**
     * Siparişleri listele
     */
    @Transactional(readOnly = true)
    public List<StockOrder> getOrdersByStatus(StockOrder.OrderStatus status) {
        return stockOrderRepository.findByStatus(status);
    }

    /**
     * Bekleyen siparişleri listele
     */
    @Transactional(readOnly = true)
    public List<StockOrder> getPendingOrders() {
        return stockOrderRepository.findPendingOrders();
    }

    /**
     * Tedarikçiye göre siparişleri listele
     */
    @Transactional(readOnly = true)
    public List<StockOrder> getOrdersBySupplier(String supplierName) {
        return stockOrderRepository.findBySupplierNameContainingIgnoreCase(supplierName);
    }

    /**
     * Sipariş numarası ile getir
     */
    @Transactional(readOnly = true)
    public Optional<StockOrder> getOrderByNumber(String orderNumber) {
        return stockOrderRepository.findByOrderNumber(orderNumber);
    }

    /**
     * Vadesi geçen siparişleri listele
     */
    @Transactional(readOnly = true)
    public List<StockOrder> getOverdueOrders() {
        return stockOrderRepository.findOverdueOrders(LocalDateTime.now());
    }

    /**
     * Kullanıcının siparişlerini listele
     */
    @Transactional(readOnly = true)
    public List<StockOrder> getOrdersByUser(String userId) {
        return stockOrderRepository.findByCreatedByOrderByOrderDateDesc(userId);
    }

    /**
     * Sipariş iptal etme
     */
    @Transactional
    public StockOrder cancelOrder(Long orderId, String reason) {
        log.info("Sipariş iptal ediliyor: orderId={}, reason={}", orderId, reason);

        StockOrder order = stockOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Sipariş bulunamadı: " + orderId));

        if (!order.isCancellable()) {
            throw new IllegalStateException("Sipariş iptal edilemez durumda: " + order.getStatus());
        }

        order.setStatus(StockOrder.OrderStatus.CANCELLED);
        order.setNotes(order.getNotes() + " | İptal nedeni: " + reason);

        StockOrder cancelledOrder = stockOrderRepository.save(order);
        log.info("Sipariş iptal edildi: orderId={}", orderId);

        return cancelledOrder;
    }

    /**
     * Sipariş request validasyonu
     */
    private void validateOrderRequest(StockOrderRequest orderRequest) {
        if (orderRequest.getSupplierName() == null || orderRequest.getSupplierName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tedarikçi adı boş olamaz");
        }

        if (orderRequest.getItems() == null || orderRequest.getItems().isEmpty()) {
            throw new IllegalArgumentException("Sipariş kalemleri boş olamaz");
        }

        for (StockOrderItemRequest item : orderRequest.getItems()) {
            if (item.getBookId() == null) {
                throw new IllegalArgumentException("Kitap ID'si boş olamaz");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Miktar pozitif olmalıdır");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Birim fiyat pozitif olmalıdır");
            }
        }
    }

    /**
     * Sipariş numarası oluşturucu
     */
    private String generateOrderNumber() {
        String orderNumber;
        do {
            orderNumber = "ORD-" + System.currentTimeMillis();
        } while (stockOrderRepository.existsByOrderNumber(orderNumber));

        return orderNumber;
    }

    /**
     * Event ID oluşturucu
     */
    private String generateEventId(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}