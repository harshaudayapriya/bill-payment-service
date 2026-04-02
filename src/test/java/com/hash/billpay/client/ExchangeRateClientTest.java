package com.hash.billpay.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hash.billpay.dto.ExchangeRateEntry;
import com.hash.billpay.exception.ExchangeRateApiException;
import com.hash.billpay.exception.ExchangeRateNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for ExchangeRateClient — tests JSON parsing, rate selection logic,
 * 6-month date filtering, and error handling without Spring context or Resilience4j.
 */
@ExtendWith(MockitoExtension.class)
class ExchangeRateClientTest {

    private static final String API_URL = "https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/rates_of_exchange";
    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestHeadersUriSpec<?> uriSpec;
    @Mock
    private RestClient.RequestHeadersSpec<?> headersSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;
    private ExchangeRateClient client;

    @BeforeEach
    void setUp() {
        client = new ExchangeRateClient(restClient, new ObjectMapper());
        ReflectionTestUtils.setField(client, "apiUrl", API_URL);

        doReturn(uriSpec).when(restClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString());
        doReturn(responseSpec).when(headersSpec).retrieve();
    }

    private void stubResponse(String json) {
        doReturn(json).when(responseSpec).body(String.class);
    }

    @Nested
    @DisplayName("Rate selection — must be ≤ purchase date")
    class RateSelection {

        @Test
        @DisplayName("Should select the most recent rate ≤ the purchase date")
        void shouldSelectMostRecentRateBeforeOrOnPurchaseDate() {
            String json = """
                    {
                        "data": [
                            {"country":"Canada","currency":"Dollar","country_currency_desc":"Canada-Dollar","exchange_rate":"1.40","effective_date":"2025-03-01"},
                            {"country":"Canada","currency":"Dollar","country_currency_desc":"Canada-Dollar","exchange_rate":"1.38","effective_date":"2025-02-15"},
                            {"country":"Canada","currency":"Dollar","country_currency_desc":"Canada-Dollar","exchange_rate":"1.35","effective_date":"2025-01-01"}
                        ]
                    }
                    """;
            stubResponse(json);

            ExchangeRateEntry result = client.getExchangeRate("Canada-Dollar", LocalDate.of(2025, 3, 15));

            assertThat(result.getExchangeRate()).isEqualByComparingTo("1.40");
            assertThat(result.getEffectiveDate()).isEqualTo(LocalDate.of(2025, 3, 1));
        }

        @Test
        @DisplayName("Should select exact date match when available")
        void shouldSelectExactDateMatch() {
            String json = """
                    {
                        "data": [
                            {"country":"Canada","currency":"Dollar","country_currency_desc":"Canada-Dollar","exchange_rate":"1.40","effective_date":"2025-03-15"},
                            {"country":"Canada","currency":"Dollar","country_currency_desc":"Canada-Dollar","exchange_rate":"1.35","effective_date":"2025-02-01"}
                        ]
                    }
                    """;
            stubResponse(json);

            ExchangeRateEntry result = client.getExchangeRate("Canada-Dollar", LocalDate.of(2025, 3, 15));

            assertThat(result.getExchangeRate()).isEqualByComparingTo("1.40");
            assertThat(result.getEffectiveDate()).isEqualTo(LocalDate.of(2025, 3, 15));
        }

        @Test
        @DisplayName("Should NOT select a rate after the purchase date")
        void shouldNotSelectRateAfterPurchaseDate() {
            // Only rate available is after the purchase date — should throw
            String json = """
                    {
                        "data": [
                            {"country":"Canada","currency":"Dollar","country_currency_desc":"Canada-Dollar","exchange_rate":"1.40","effective_date":"2025-04-01"}
                        ]
                    }
                    """;
            stubResponse(json);

            assertThatThrownBy(() -> client.getExchangeRate("Canada-Dollar", LocalDate.of(2025, 3, 15)))
                    .isInstanceOf(ExchangeRateNotFoundException.class);
        }

        @Test
        @DisplayName("Should select correct rate when multiple rates exist, some after purchase date")
        void shouldFilterOutFutureRates() {
            String json = """
                    {
                        "data": [
                            {"country":"Canada","currency":"Dollar","country_currency_desc":"Canada-Dollar","exchange_rate":"1.42","effective_date":"2025-04-01"},
                            {"country":"Canada","currency":"Dollar","country_currency_desc":"Canada-Dollar","exchange_rate":"1.38","effective_date":"2025-03-10"},
                            {"country":"Canada","currency":"Dollar","country_currency_desc":"Canada-Dollar","exchange_rate":"1.35","effective_date":"2025-02-01"}
                        ]
                    }
                    """;
            stubResponse(json);

            ExchangeRateEntry result = client.getExchangeRate("Canada-Dollar", LocalDate.of(2025, 3, 15));

            // Should pick 2025-03-10 (most recent ≤ 2025-03-15), NOT 2025-04-01
            assertThat(result.getExchangeRate()).isEqualByComparingTo("1.38");
            assertThat(result.getEffectiveDate()).isEqualTo(LocalDate.of(2025, 3, 10));
        }
    }

    @Nested
    @DisplayName("API URL construction — 6-month window")
    class ApiUrlConstruction {

        @Test
        @DisplayName("Should construct URL with correct 6-month date filter")
        void shouldBuildUrlWithSixMonthFilter() {
            stubResponse("""
                    {"data": [{"country":"Canada","currency":"Dollar","country_currency_desc":"Canada-Dollar","exchange_rate":"1.35","effective_date":"2025-03-01"}]}
                    """);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

            client.getExchangeRate("Canada-Dollar", LocalDate.of(2025, 6, 15));

            verify(uriSpec).uri(urlCaptor.capture());
            String url = urlCaptor.getValue();

            assertThat(url).contains("effective_date:gte:2024-12-15");
            assertThat(url).contains("effective_date:lte:2025-06-15");
            assertThat(url).contains("country_currency_desc:eq:Canada-Dollar");
            assertThat(url).contains("sort=-effective_date");
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should throw ExchangeRateNotFoundException when data array is empty")
        void shouldThrowWhenEmptyData() {
            stubResponse("""
                    {"data": [], "meta": {"count": 0}}
                    """);

            assertThatThrownBy(() -> client.getExchangeRate("NonExistent-Currency", LocalDate.of(2025, 3, 15)))
                    .isInstanceOf(ExchangeRateNotFoundException.class)
                    .hasMessageContaining("cannot be converted");
        }

        @Test
        @DisplayName("Should throw ExchangeRateNotFoundException when data field is missing")
        void shouldThrowWhenNoDataField() {
            stubResponse("""
                    {"meta": {"count": 0}}
                    """);

            assertThatThrownBy(() -> client.getExchangeRate("Canada-Dollar", LocalDate.of(2025, 3, 15)))
                    .isInstanceOf(ExchangeRateNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw ExchangeRateApiException on malformed JSON")
        void shouldThrowOnMalformedJson() {
            stubResponse("not valid json {{{");

            assertThatThrownBy(() -> client.getExchangeRate("Canada-Dollar", LocalDate.of(2025, 3, 15)))
                    .isInstanceOf(ExchangeRateApiException.class)
                    .hasMessageContaining("Failed to parse");
        }

        @Test
        @DisplayName("Should throw ExchangeRateApiException on invalid exchange_rate number")
        void shouldThrowOnInvalidNumber() {
            stubResponse("""
                    {"data": [{"country":"X","currency":"Y","country_currency_desc":"X-Y","exchange_rate":"not-a-number","effective_date":"2025-03-01"}]}
                    """);

            assertThatThrownBy(() -> client.getExchangeRate("X-Y", LocalDate.of(2025, 3, 15)))
                    .isInstanceOf(ExchangeRateApiException.class);
        }
    }

    @Nested
    @DisplayName("Successful parsing")
    class SuccessfulParsing {

        @Test
        @DisplayName("Should correctly parse all fields from the API response")
        void shouldParseAllFields() {
            stubResponse("""
                    {
                        "data": [{
                            "country": "United Kingdom",
                            "currency": "Pound",
                            "country_currency_desc": "United Kingdom-Pound",
                            "exchange_rate": "0.79",
                            "effective_date": "2025-03-01"
                        }]
                    }
                    """);

            ExchangeRateEntry result = client.getExchangeRate("United Kingdom-Pound", LocalDate.of(2025, 3, 15));

            assertThat(result.getCountry()).isEqualTo("United Kingdom");
            assertThat(result.getCurrency()).isEqualTo("Pound");
            assertThat(result.getCountryCurrencyDesc()).isEqualTo("United Kingdom-Pound");
            assertThat(result.getExchangeRate()).isEqualByComparingTo("0.79");
            assertThat(result.getEffectiveDate()).isEqualTo(LocalDate.of(2025, 3, 1));
        }

        @Test
        @DisplayName("Should handle single entry response")
        void shouldHandleSingleEntry() {
            stubResponse("""
                    {"data": [{"country":"Japan","currency":"Yen","country_currency_desc":"Japan-Yen","exchange_rate":"149.50","effective_date":"2025-03-10"}]}
                    """);

            ExchangeRateEntry result = client.getExchangeRate("Japan-Yen", LocalDate.of(2025, 3, 15));

            assertThat(result.getExchangeRate()).isEqualByComparingTo("149.50");
        }
    }
}

