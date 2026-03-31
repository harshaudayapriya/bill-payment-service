package com.hash.billpay.exception;

import java.time.LocalDate;

/**
 * Thrown when the exchange rate for a requested currency cannot be found.
 */
public class ExchangeRateNotFoundException extends RuntimeException {

    public ExchangeRateNotFoundException(String currency) {
        super("Exchange rate not found for currency: " + currency);
    }

    public ExchangeRateNotFoundException(String currency, LocalDate transactionDate) {
        super("The purchase cannot be converted to the target currency '" + currency
                + "'. No exchange rate is available within 6 months equal to or before the purchase date " + transactionDate);
    }
}
