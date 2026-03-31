package com.hash.billpay.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hash.billpay.config.SecurityConfig;
import com.hash.billpay.config.TestConfig;
import com.hash.billpay.dto.PurchaseTransactionRequest;
import com.hash.billpay.dto.PurchaseTransactionResponse;
import com.hash.billpay.exception.DuplicateTransactionException;
import com.hash.billpay.model.BillerType;
import com.hash.billpay.service.PurchaseTransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PurchaseTransactionController.class)
@Import({SecurityConfig.class, TestConfig.class})
@TestPropertySource(properties = "api.security.api-key=test-api-key")
class PurchaseTransactionControllerTest {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String VALID_API_KEY = "test-api-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PurchaseTransactionService transactionService;

    @Nested
    @DisplayName("API Key Authentication")
    class ApiKeySecurity {

        @Test
        @DisplayName("Should return 401 when API key is missing")
        void shouldRejectRequestWithoutApiKey() throws Exception {
            mockMvc.perform(get("/api/v1/transactions"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 when API key is invalid")
        void shouldRejectRequestWithInvalidApiKey() throws Exception {
            mockMvc.perform(get("/api/v1/transactions")
                            .header(API_KEY_HEADER, "wrong-key"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/transactions")
    class CreateTransaction {

        @Test
        @DisplayName("Should create a transaction successfully")
        void shouldCreateTransaction() throws Exception {
            UUID id = UUID.randomUUID();
            PurchaseTransactionRequest request = PurchaseTransactionRequest.builder()
                    .idempotencyKey(UUID.randomUUID())
                    .description("Electric bill payment")
                    .transactionDate(LocalDate.of(2025, 3, 15))
                    .purchaseAmount(new BigDecimal("150.75"))
                    .billerType(BillerType.ELECTRICITY)
                    .build();

            PurchaseTransactionResponse response = PurchaseTransactionResponse.builder()
                    .id(id)
                    .description("Electric bill payment")
                    .transactionDate(LocalDate.of(2025, 3, 15))
                    .purchaseAmount(new BigDecimal("150.75"))
                    .billerType(BillerType.ELECTRICITY)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(transactionService.createTransaction(any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/transactions")
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.description").value("Electric bill payment"))
                    .andExpect(jsonPath("$.purchaseAmount").value(150.75))
                    .andExpect(jsonPath("$.billerType").value("ELECTRICITY"));
        }

        @Test
        @DisplayName("Should return 400 when description is blank")
        void shouldFailWhenDescriptionBlank() throws Exception {
            PurchaseTransactionRequest request = PurchaseTransactionRequest.builder()
                    .idempotencyKey(UUID.randomUUID())
                    .description("")
                    .transactionDate(LocalDate.of(2025, 3, 15))
                    .purchaseAmount(new BigDecimal("150.75"))
                    .billerType(BillerType.ELECTRICITY)
                    .build();

            mockMvc.perform(post("/api/v1/transactions")
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when description exceeds 50 characters")
        void shouldFailWhenDescriptionTooLong() throws Exception {
            PurchaseTransactionRequest request = PurchaseTransactionRequest.builder()
                    .idempotencyKey(UUID.randomUUID())
                    .description("A".repeat(51))
                    .transactionDate(LocalDate.of(2025, 3, 15))
                    .purchaseAmount(new BigDecimal("150.75"))
                    .billerType(BillerType.ELECTRICITY)
                    .build();

            mockMvc.perform(post("/api/v1/transactions")
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when purchase amount is negative")
        void shouldFailWhenAmountNegative() throws Exception {
            PurchaseTransactionRequest request = PurchaseTransactionRequest.builder()
                    .idempotencyKey(UUID.randomUUID())
                    .description("Test payment")
                    .transactionDate(LocalDate.of(2025, 3, 15))
                    .purchaseAmount(new BigDecimal("-10.00"))
                    .billerType(BillerType.WATER)
                    .build();

            mockMvc.perform(post("/api/v1/transactions")
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when purchase amount is zero")
        void shouldFailWhenAmountZero() throws Exception {
            PurchaseTransactionRequest request = PurchaseTransactionRequest.builder()
                    .idempotencyKey(UUID.randomUUID())
                    .description("Test payment")
                    .transactionDate(LocalDate.of(2025, 3, 15))
                    .purchaseAmount(BigDecimal.ZERO)
                    .billerType(BillerType.WATER)
                    .build();

            mockMvc.perform(post("/api/v1/transactions")
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when transaction date is null")
        void shouldFailWhenDateNull() throws Exception {
            PurchaseTransactionRequest request = PurchaseTransactionRequest.builder()
                    .idempotencyKey(UUID.randomUUID())
                    .description("Test payment")
                    .purchaseAmount(new BigDecimal("50.00"))
                    .billerType(BillerType.GAS)
                    .build();

            mockMvc.perform(post("/api/v1/transactions")
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when biller type is null")
        void shouldFailWhenBillerTypeNull() throws Exception {
            PurchaseTransactionRequest request = PurchaseTransactionRequest.builder()
                    .idempotencyKey(UUID.randomUUID())
                    .description("Test payment")
                    .transactionDate(LocalDate.of(2025, 3, 15))
                    .purchaseAmount(new BigDecimal("50.00"))
                    .billerType(null)
                    .build();

            mockMvc.perform(post("/api/v1/transactions")
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when transaction date has invalid format")
        void shouldFailWhenDateFormatInvalid() throws Exception {
            String invalidJson = """
                    {
                        "billerType": "ELECTRICITY",
                        "purchaseAmount": 50.00,
                        "transactionDate": "not-a-date",
                        "description": "Test payment"
                    }
                    """;

            mockMvc.perform(post("/api/v1/transactions")
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid Request Body"))
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Invalid date format")));
        }

        @Test
        @DisplayName("Should return 400 when transaction date has invalid day/month values")
        void shouldFailWhenDateValuesInvalid() throws Exception {
            String invalidJson = """
                    {
                        "billerType": "ELECTRICITY",
                        "purchaseAmount": 50.00,
                        "transactionDate": "2025-13-45",
                        "description": "Test payment"
                    }
                    """;

            mockMvc.perform(post("/api/v1/transactions")
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid Request Body"));
        }

        @Test
        @DisplayName("Should return 400 when idempotency key is null")
        void shouldFailWhenIdempotencyKeyNull() throws Exception {
            PurchaseTransactionRequest request = PurchaseTransactionRequest.builder()
                    .idempotencyKey(null)
                    .description("Test payment")
                    .transactionDate(LocalDate.of(2025, 3, 15))
                    .purchaseAmount(new BigDecimal("50.00"))
                    .billerType(BillerType.ELECTRICITY)
                    .build();

            mockMvc.perform(post("/api/v1/transactions")
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 409 when idempotency key is a duplicate")
        void shouldFailWhenDuplicateIdempotencyKey() throws Exception {
            UUID duplicateKey = UUID.randomUUID();
            PurchaseTransactionRequest request = PurchaseTransactionRequest.builder()
                    .idempotencyKey(duplicateKey)
                    .description("Test payment")
                    .transactionDate(LocalDate.of(2025, 3, 15))
                    .purchaseAmount(new BigDecimal("50.00"))
                    .billerType(BillerType.ELECTRICITY)
                    .build();

            when(transactionService.createTransaction(any()))
                    .thenThrow(new DuplicateTransactionException(duplicateKey));

            mockMvc.perform(post("/api/v1/transactions")
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Duplicate Transaction"))
                    .andExpect(jsonPath("$.detail").value(
                            org.hamcrest.Matchers.containsString(duplicateKey.toString())));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/transactions/{id}")
    class GetTransaction {

        @Test
        @DisplayName("Should return a transaction by ID")
        void shouldReturnTransaction() throws Exception {
            UUID id = UUID.randomUUID();
            PurchaseTransactionResponse response = PurchaseTransactionResponse.builder()
                    .id(id)
                    .description("Internet bill")
                    .transactionDate(LocalDate.of(2025, 2, 10))
                    .purchaseAmount(new BigDecimal("89.99"))
                    .billerType(BillerType.INTERNET)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(transactionService.getTransaction(id)).thenReturn(response);

            mockMvc.perform(get("/api/v1/transactions/{id}", id)
                            .header(API_KEY_HEADER, VALID_API_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.description").value("Internet bill"))
                    .andExpect(jsonPath("$.billerType").value("INTERNET"));
        }
    }
}
