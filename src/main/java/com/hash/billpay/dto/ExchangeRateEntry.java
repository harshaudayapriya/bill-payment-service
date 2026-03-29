package com.hash.billpay.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO representing a single exchange rate entry from the external API.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRateEntry {

    private String country;
    private String currency;
    private String countryCurrencyDesc;
    private BigDecimal exchangeRate;
    private LocalDate effectiveDate;
}
