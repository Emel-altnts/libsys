package com.d_tech.libsys.dto;

import com.d_tech.libsys.dto.InvoiceRequest;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Stok sipariş isteği DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockOrderRequest {

    private String supplierName;
    private String supplierContact;
    private LocalDateTime expectedDeliveryDate;
    private String notes;
    private String createdBy;
    private List<StockOrderItemRequest> items;
}

