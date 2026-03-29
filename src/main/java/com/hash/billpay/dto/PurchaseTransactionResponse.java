package com.hash.billpay.dto;

import com.hash.billpay.model.BillerType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a purchase transaction (original currency - USD).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseTransactionResponse {

    private UUID id;
    private String description;
    private LocalDate transactionDate;
    private BigDecimal purchaseAmount;
    private BillerType billerType;
    private LocalDateTime createdAt;
}