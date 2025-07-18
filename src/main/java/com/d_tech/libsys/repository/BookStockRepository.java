package com.d_tech.libsys.repository;

import com.d_tech.libsys.domain.model.BookStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * BookStock repository
 */
@Repository
public interface BookStockRepository extends JpaRepository<BookStock, Long> {

    /**
     * Kitaba göre stok bilgisini bulur
     */
    Optional<BookStock> findByBookId(Long bookId);

    /**
     * Düşük stoklu kitapları bulur
     */
    @Query("SELECT bs FROM BookStock bs WHERE bs.status = 'LOW_STOCK' OR bs.status = 'OUT_OF_STOCK'")
    List<BookStock> findLowStockBooks();

    /**
     * Stok durumuna göre kitapları bulur
     */
    List<BookStock> findByStatus(BookStock.StockStatus status);

    /**
     * Minimum stok altındaki kitapları bulur
     */
    @Query("SELECT bs FROM BookStock bs WHERE bs.currentQuantity <= bs.minimumQuantity")
    List<BookStock> findBooksNeedingRestock();

    /**
     * Tedarikçiye göre stok bilgilerini bulur
     */
    List<BookStock> findBySupplierNameContainingIgnoreCase(String supplierName);

    /**
     * Stok miktarı aralığında olan kitapları bulur
     */
    @Query("SELECT bs FROM BookStock bs WHERE bs.currentQuantity BETWEEN :minQty AND :maxQty")
    List<BookStock> findByQuantityRange(@Param("minQty") Integer minQuantity, @Param("maxQty") Integer maxQuantity);

    /**
     * Toplam stok değeri hesaplar
     */
    @Query("SELECT COALESCE(SUM(bs.currentQuantity * bs.unitPrice), 0) FROM BookStock bs")
    Double calculateTotalStockValue();

    /**
     * Kitap var mı kontrolü
     */
    boolean existsByBookId(Long bookId);
}

