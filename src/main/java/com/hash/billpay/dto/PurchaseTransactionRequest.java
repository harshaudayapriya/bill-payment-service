package com.hash.billpay.dto;

import com.hash.billpay.model.BillerType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for creating a new purchase transaction.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class PurchaseTransactionRequest {

    /**
     * Client-generated unique key to prevent duplicate transaction submissions.
     * Must be a valid UUID. Sending the same key twice will return 409 Conflict.
     */
    @NotNull(message = "Idempotency key is required")
    private UUID idempotencyKey;

    @NotNull(message = "Biller type is required")
    private BillerType billerType;

    @Digits(integer = 15, fraction = 2, message = "Purchase amount must be rounded to the nearest cent")
    @DecimalMin(value = "0.01", message = "Purchase amount must be a positive value")
    @NotNull(message = "Purchase amount is required")
    private BigDecimal purchaseAmount;

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    @Size(max = 50, message = "Description must not exceed 50 characters")
    @NotBlank(message = "Description is required")
    private String description;
}