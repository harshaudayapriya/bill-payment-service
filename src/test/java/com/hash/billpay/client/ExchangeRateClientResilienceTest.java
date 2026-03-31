package com.hash.billpay.client;

import com.hash.billpay.exception.ExchangeRateNotFoundException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration tests for Resilience4j circuit breaker and retry behavior on ExchangeRateClient.
 *
 * <p>Uses a real Spring context so that Resilience4j annotations are proxied and active.
 * The RestClient is spied on to simulate external API failures.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        // Disable retry in most tests (override per-test where needed)
        "resilience4j.retry.instances.exchangeRate.max-attempts=1",
        // Make circuit breaker open quickly for testing
        "resilience4j.circuitbreaker.instances.exchangeRate.minimum-number-of-calls=3",
        "resilience4j.circuitbreaker.instances.exchangeRate.sliding-window-size=5",
        "resilience4j.circuitbreaker.instances.exchangeRate.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.exchangeRate.wait-duration-in-open-state=60s"
})
class ExchangeRateClientResilienceTest {

    private static final String CURRENCY = "Canada-Dollar";
    private static final LocalDate TX_DATE = LocalDate.of(2025, 3, 15);
    @Autowired
    private ExchangeRateClient exchangeRateClient;
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    @MockitoSpyBean
    private RestClient restClient;

    @BeforeEach
    void resetCircuitBreaker() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("exchangeRate");
        cb.reset();
    }

    /**
     * Helper to stub the RestClient to throw a given exception on GET.
     */
    private void stubRestClientToThrow(Throwable throwable) {
        RestClient.RequestHeadersUriSpec<?> uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec<?> headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(uriSpec).when(restClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString());
        doReturn(responseSpec).when(headersSpec).retrieve();
        doThrow(throwable).when(responseSpec).body(String.class);
    }

    /**
     * Helper to stub the RestClient to return a given JSON string.
     */
    private void stubRestClientToReturn(String json) {
        RestClient.RequestHeadersUriSpec<?> uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec<?> headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(uriSpec).when(restClient).get();
        doReturn(headersSpec).when(uriSpec).uri(anyString());
        doReturn(responseSpec).when(headersSpec).retrieve();
        doReturn(json).when(responseSpec).body(String.class);
    }

    @Nested
    @DisplayName("Circuit Breaker")
    class CircuitBreakerTests {

        @Test
        @DisplayName("Circuit breaker should start in CLOSED state")
        void shouldStartClosed() {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("exchangeRate");
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("Circuit breaker should open after enough failures")
        void shouldOpenAfterFailures() {
            stubRestClientToThrow(new ResourceAccessException("Connection refused",
                    new IOException("Connection refused")));

            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("exchangeRate");

            // Make enough calls to trip the circuit breaker (minimum-number-of-calls=3, threshold=50%)
            for (int i = 0; i < 5; i++) {
                try {
                    exchangeRateClient.getExchangeRate(CURRENCY, TX_DATE.plusDays(i));
                } catch (Exception ignored) {
                    // Expected to fail
                }
            }

            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }


        @Test
        @DisplayName("ExchangeRateNotFoundException should NOT trip the circuit breaker")
        void notFoundShouldNotTripCircuitBreaker() {
            // Return empty data array → triggers ExchangeRateNotFoundException
            String emptyResponse = """
                    {"data": [], "meta": {"count": 0}}
                    """;
            stubRestClientToReturn(emptyResponse);

            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("exchangeRate");

            for (int i = 0; i < 5; i++) {
                try {
                    exchangeRateClient.getExchangeRate("NonExistent-Currency", TX_DATE.plusDays(i));
                } catch (ExchangeRateNotFoundException ignored) {
                }
            }

            // Circuit breaker should still be CLOSED because ExchangeRateNotFoundException is ignored
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }
    }

    @Nested
    @DisplayName("Retry")
    class RetryTests {

        @Test
        @DisplayName("Should succeed on valid response without retries")
        void shouldSucceedWithoutRetry() {
            String validResponse = """
                    {
                        "data": [{
                            "country": "Canada",
                            "currency": "Dollar",
                            "country_currency_desc": "Canada-Dollar",
                            "exchange_rate": "1.35",
                            "effective_date": "2025-03-15"
                        }]
                    }
                    """;
            stubRestClientToReturn(validResponse);

            var result = exchangeRateClient.getExchangeRate(CURRENCY, TX_DATE);

            assertThat(result).isNotNull();
            assertThat(result.getCountryCurrencyDesc()).isEqualTo("Canada-Dollar");
            assertThat(result.getExchangeRate()).isEqualByComparingTo("1.35");
        }

        @Test
        @DisplayName("ExchangeRateNotFoundException should NOT be retried")
        void notFoundShouldNotRetry() {
            String emptyResponse = """
                    {"data": [], "meta": {"count": 0}}
                    """;
            stubRestClientToReturn(emptyResponse);

            assertThatThrownBy(() -> exchangeRateClient.getExchangeRate("NonExistent-Currency", TX_DATE))
                    .isInstanceOf(ExchangeRateNotFoundException.class);

            // Should have been called exactly once (no retries)
            verify(restClient, times(1)).get();
        }
    }

    @Nested
    @DisplayName("Fallback")
    class FallbackTests {

        @Test
        @DisplayName("ExchangeRateNotFoundException should propagate through fallback")
        void notFoundShouldPropagateFromFallback() {
            String emptyResponse = """
                    {"data": [], "meta": {"count": 0}}
                    """;
            stubRestClientToReturn(emptyResponse);

            assertThatThrownBy(() -> exchangeRateClient.getExchangeRate("NonExistent-Currency", TX_DATE))
                    .isInstanceOf(ExchangeRateNotFoundException.class)
                    .hasMessageContaining("NonExistent-Currency");
        }
    }
}
