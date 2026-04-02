package com.hash.billpay.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO when retrieving a purchase transaction converted to a target currency.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConvertedTransactionResponse {

    private UUID id;
    private String description;
    private LocalDate transactionDate;

    /**
     * Original purchase amount in USD.
     */
    private BigDecimal originalAmountUsd;

    /**
     * The exchange rate used for conversion.
     */
    private BigDecimal exchangeRate;

    /**
     * Converted amount in the target currency, rounded to 2 decimal places.
     */
    private BigDecimal convertedAmount;

    /**
     * The target currency code (e.g. "EUR", "GBP").
     */
    private String targetCurrency;

    /**
     * The date of the exchange rate used.
     */
    private LocalDate exchangeRateDate;
}