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
     * 🚀 FIXED: Sipariş detayını getir - SIMPLIFIED AND ROBUST VERSION
     */
    @Transactional(readOnly = true)
    public Optional<StockOrder> getOrderById(Long orderId) {
        System.out.println("=== StockOrderService.getOrderById SIMPLIFIED VERSION ===");
        System.out.println("🔍 Aranan ID: " + orderId);

        log.info("Sipariş detayı getiriliyor: orderId={}", orderId);

        try {
            // ✅ Input validation
            if (orderId == null || orderId <= 0) {
                System.out.println("❌ Geçersiz orderId: " + orderId);
                log.warn("Geçersiz orderId: {}", orderId);
                return Optional.empty();
            }

            // ✅ Simple approach - direct repository call
            System.out.println("🔍 Repository.findById çağrılıyor...");
            Optional<StockOrder> result = stockOrderRepository.findById(orderId);

            if (result.isPresent()) {
                StockOrder order = result.get();
                System.out.println("✅ Sipariş bulundu: ID=" + order.getId() + ", OrderNumber=" + order.getOrderNumber());
                log.info("Sipariş bulundu: orderId={}, orderNumber={}", orderId, order.getOrderNumber());
                return result;
            } else {
                System.out.println("❌ Sipariş bulunamadı - alternatif yöntemler deneniyor...");
                
                // Try with native query as fallback
                try {
                    StockOrder order = (StockOrder) entityManager.createNativeQuery(
                            "SELECT * FROM stock_orders WHERE id = ?", StockOrder.class)
                            .setParameter(1, orderId)
                            .getSingleResult();
                    
                    System.out.println("✅ Native query ile bulundu: " + order.getOrderNumber());
                    return Optional.of(order);
                } catch (Exception nativeEx) {
                    System.out.println("❌ Native query ile de bulunamadı");
                }
                
                // Final debug info
                printSimpleDebugInfo(orderId);
                return Optional.empty();
            }

        } catch (Exception e) {
            System.out.println("💥 getOrderById hatası: " + e.getMessage());
            log.error("Sipariş detayı getirme hatası: orderId={}, error={}", orderId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * ✅ SIMPLIFIED DEBUG: Basit debug bilgisi
     */
    private void printSimpleDebugInfo(Long searchId) {
        try {
            System.out.println("=== SIMPLE DEBUG INFO ===");

            // Toplam sipariş sayısı
            long totalCount = stockOrderRepository.count();
            System.out.println("📊 Toplam sipariş sayısı: " + totalCount);

            if (totalCount > 0) {
                // İlk 3 sipariş ID'si
                List<Long> firstIds = stockOrderRepository.debugGetAllIds().stream().limit(3).toList();
                System.out.println("📊 İlk 3 ID: " + firstIds);
                
                // Search ID var mı kontrol
                List<Long> allIds = stockOrderRepository.debugGetAllIds();
                boolean exists = allIds.contains(searchId);
                System.out.println("🔍 Search ID (" + searchId + ") exists: " + exists);
                
                if (!exists && !allIds.isEmpty()) {
                    System.out.println("💡 Suggestion: Mevcut ID'lerden birini deneyin: " + allIds.get(0));
                }
            } else {
                System.out.println("⚠️ Veritabanında hiç sipariş yok!");
            }

        } catch (Exception debugEx) {
            System.out.println("⚠️ Debug info hatası: " + debugEx.getMessage());
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
     * ✅ DEBUG: Test siparişi oluştur (debug için)
     */
    @Transactional
    public StockOrder createTestOrderForDebug() {
        log.info("DEBUG: Test siparişi oluşturuluyor");

        try {
            // Test siparişi oluştur
            StockOrder testOrder = StockOrder.builder()
                    .orderNumber("TEST-" + System.currentTimeMillis())
                    .supplierName("Test Supplier")
                    .supplierContact("test@supplier.com")
                    .expectedDeliveryDate(LocalDateTime.now().plusDays(7))
                    .notes("Test siparişi - Debug amaçlı oluşturuldu")
                    .createdBy("SYSTEM_DEBUG")
                    .status(StockOrder.OrderStatus.PENDING)
                    .totalAmount(BigDecimal.valueOf(100.00))
                    .totalVat(BigDecimal.valueOf(18.00))
                    .grandTotal(BigDecimal.valueOf(118.00))
                    .build();

            StockOrder savedOrder = stockOrderRepository.save(testOrder);
            log.info("DEBUG: Test siparişi oluşturuldu - ID: {}, OrderNumber: {}", 
                    savedOrder.getId(), savedOrder.getOrderNumber());

            return savedOrder;
        } catch (Exception e) {
            log.error("DEBUG: Test siparişi oluşturma hatası: {}", e.getMessage(), e);
            throw new RuntimeException("Test siparişi oluşturulamadı", e);
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