package com.hash.billpay.client;

import com.hash.billpay.dto.ExchangeRateEntry;

import java.time.LocalDate;

public interface ExchangeRateProvider {
    /**
     * Fetch the most recent exchange rate for the given currency that is less than or equal to
     * the transaction date and within the last 6 months
     */
    ExchangeRateEntry getExchangeRate(String currency, LocalDate transactionDate);
}
