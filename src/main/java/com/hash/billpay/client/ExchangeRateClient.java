package com.hash.billpay.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hash.billpay.dto.ExchangeRateEntry;
import com.hash.billpay.exception.ExchangeRateApiException;
import com.hash.billpay.exception.ExchangeRateNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Client that retriev exchange rates from the US Treasury Fiscal Data API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateClient {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    @Value("${exchange-rate.api-url}")
    private String apiUrl;

    /**
     * Fetch the exchange rate for the given currency closest to the transaction date.
     *
     * @param currency        the target currency description (e.g., "Canada-Dollar", "Euro Zone-Euro")
     * @param transactionDate the date of the purchase transaction
     * @return the closest matching ExchangeRateEntry
     */
    public ExchangeRateEntry getExchangeRate(String currency, LocalDate transactionDate) {
        try {
            // Query rates for the given currency, sorted by effective_date descending
            String url = apiUrl
                    + "?fields=country_currency_desc,exchange_rate,record_date,effective_date,country,currency"
                    + "&filter=country_currency_desc:eq:" + currency
                    + "&sort=-effective_date"
                    + "&page[size]=200";

            log.info("Fetching exchange rates from: {}", url);

            String responseBody = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode data = root.path("data");

            if (data == null || !data.isArray() || data.isEmpty()) {
                throw new ExchangeRateNotFoundException(currency);
            }

            // Parse all entries and find the one closest to the transaction date
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

            // Find the entry with the closest date to the transaction date
            return entries.stream()
                    .min(Comparator.comparingLong(e ->
                            Math.abs(ChronoUnit.DAYS.between(e.getEffectiveDate(), transactionDate))))
                    .orElseThrow(() -> new ExchangeRateNotFoundException(currency));

        } catch (ExchangeRateNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching exchange rate for currency: {}", currency, e);
            throw new ExchangeRateApiException(
                    "Failed to fetch exchange rate for currency: " + currency, e);
        }
    }
}
