package com.hash.billpay.exception;

/**
 * Thrown when the external exchange rate API is unreachable or returns an error.
 */
public class ExchangeRateApiException extends RuntimeException {

    public ExchangeRateApiException(String message) {
        super(message);
    }

    public ExchangeRateApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
