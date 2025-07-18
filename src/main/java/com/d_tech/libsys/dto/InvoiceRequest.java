package com.d_tech.libsys.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime; /**
 * Fatura isteği DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceRequest {

    private LocalDateTime dueDate;
    private String supplierAddress;
    private String supplierTaxNumber;
    private String supplierPhone;
    private String supplierEmail;
    private String buyerName;
    private String buyerAddress;
    private String buyerTaxNumber;
    private String notes;
    private String createdBy;
}
