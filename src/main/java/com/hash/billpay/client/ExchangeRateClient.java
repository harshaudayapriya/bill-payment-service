package com.hash.billpay.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hash.billpay.dto.ExchangeRateEntry;
import com.hash.billpay.exception.ExchangeRateApiException;
import com.hash.billpay.exception.ExchangeRateNotFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ExchangeRateProvider implementation backed by the
 * US Treasury Fiscal Data API
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class ExchangeRateClient implements ExchangeRateProvider {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    @Value("${exchange-rate.api-url}")
    private String apiUrl;

    @Override
    @Cacheable(value = "exchangeRates", key = "#currency + '_' + #transactionDate")
    @CircuitBreaker(name = "exchangeRate", fallbackMethod = "exchangeRateFallback")
    @Retry(name = "exchangeRate")
    public ExchangeRateEntry getExchangeRate(String currency, LocalDate transactionDate) {
        // Only consider rates within 6 months prior to (and up to) the transaction date
        LocalDate sixMonthsBefore = transactionDate.minusMonths(6);

        String url = apiUrl
                + "?fields=country_currency_desc,exchange_rate,record_date,effective_date,country,currency"
                + "&filter=country_currency_desc:eq:" + currency
                + ",effective_date:gte:" + sixMonthsBefore.format(DATE_FMT)
                + ",effective_date:lte:" + transactionDate.format(DATE_FMT)
                + "&sort=-effective_date"
                + "&page[size]=200";

        log.info("Fetching exchange rates from Treasury API for currency={}, transactionDate={}, dateRange=[{}, {}]",
                currency, transactionDate, sixMonthsBefore, transactionDate);

        String responseBody = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        return parseExchangeRateResponse(responseBody, currency, transactionDate);
    }

    /**
     * Fallback when the circuit breaker is open or all retries are exhausted.
     */
    private ExchangeRateEntry exchangeRateFallback(String currency, LocalDate transactionDate, Throwable t) {
        if (t instanceof ExchangeRateNotFoundException) {
            throw (ExchangeRateNotFoundException) t;
        }

        if (t instanceof CallNotPermittedException) {
            log.warn("Circuit breaker is OPEN for exchange rate service. currency={}", currency);
            throw new ExchangeRateApiException(
                    "Exchange rate service is temporarily unavailable (circuit breaker open). Please try again later.", t);
        }

        log.error("Exchange rate lookup failed after retries. currency={}, transactionDate={}", currency, transactionDate, t);
        throw new ExchangeRateApiException(
                "Failed to fetch exchange rate for currency: " + currency + ". The service may be temporarily unavailable.", t);
    }

    /**
     * Parses the Treasury API JSON response and selects the most recent exchange rate
     * that is less than or equal to the transaction date.
     */
    private ExchangeRateEntry parseExchangeRateResponse(String responseBody, String currency, LocalDate transactionDate) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.path("data");

            if (!data.isArray() || data.isEmpty()) {
                throw new ExchangeRateNotFoundException(currency, transactionDate);
            }

            List<ExchangeRateEntry> entries = new ArrayList<>();
            for (JsonNode node : data) {
                ExchangeRateEntry entry = ExchangeRateEntry.builder()
                        .country(node.path("country").asText())
                        .currency(node.path("currency").asText())
                        .countryCurrencyDesc(node.path("country_currency_desc").asText())
                        .exchangeRate(new BigDecimal(node.path("exchange_rate").asText()))
                        .effectiveDate(LocalDate.parse(node.path("effective_date").asText(), DATE_FMT))
                        .build();
                entries.add(entry);
            }

            // Entries are sorted by effective_date descending, so the first one is the best match.
            return entries.stream()
                    .filter(e -> !e.getEffectiveDate().isAfter(transactionDate))
                    .max(Comparator.comparing(ExchangeRateEntry::getEffectiveDate))
                    .orElseThrow(() -> new ExchangeRateNotFoundException(currency, transactionDate));

        } catch (ExchangeRateNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse exchange rate response for currency={}", currency, e);
            throw new ExchangeRateApiException("Failed to parse exchange rate response for currency: " + currency, e);
        }
    }
}

