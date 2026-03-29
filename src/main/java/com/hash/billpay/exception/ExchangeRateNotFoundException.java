package com.hash.billpay.exception;

/**
 * Thrown when the exchange rate for a requested currency cannot be found.
 */
public class ExchangeRateNotFoundException extends RuntimeException {

    public ExchangeRateNotFoundException(String currency) {
        super("Exchange rate not found for currency: " + currency);
    }
}
