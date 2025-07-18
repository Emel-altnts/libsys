package com.d_tech.libsys.dto;

/**
 * Stok teslimat kalemi DTO
 */
@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class StockReceiptItem {
    private Long orderItemId;
    private Integer receivedQuantity;
    private String notes;
}
