package com.d_tech.libsys.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal; /**
 * Stok sipariş kalemi isteği DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockOrderItemRequest {

    private Long bookId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal vatRate;
    private BigDecimal discountRate;
    private String notes;
}
