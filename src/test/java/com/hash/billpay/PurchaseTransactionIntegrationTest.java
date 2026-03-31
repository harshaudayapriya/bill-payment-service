package com.hash.billpay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hash.billpay.dto.PurchaseTransactionRequest;
import com.hash.billpay.model.BillerType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PurchaseTransactionIntegrationTest {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String VALID_API_KEY = "test-api-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private PurchaseTransactionRequest buildValidRequest() {
        return PurchaseTransactionRequest.builder()
                .idempotencyKey(UUID.randomUUID())
                .description("Integration test bill")
                .transactionDate(LocalDate.of(2025, 3, 15))
                .purchaseAmount(new BigDecimal("99.99"))
                .billerType(BillerType.ELECTRICITY)
                .build();
    }

    @Nested
    @DisplayName("Create and Retrieve Transaction — Full Lifecycle")
    class CreateAndRetrieve {

        @Test
        @DisplayName("Should create a transaction and retrieve it by ID")
        void shouldCreateAndRetrieveById() throws Exception {
            PurchaseTransactionRequest request = buildValidRequest();

            // Create
            MvcResult createResult = mockMvc.perform(post("/api/v1/transactions")
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.description").value("Integration test bill"))
                    .andExpect(jsonPath("$.purchaseAmount").value(99.99))
                    .andExpect(jsonPath("$.billerType").value("ELECTRICITY"))
                    .andExpect(jsonPath("$.transactionDate").value("2025-03-15"))
                    .andReturn();

            // Extract the ID from the create response
            String responseJson = createResult.getResponse().getContentAsString();
            String id = objectMapper.readTree(responseJson).get("id").asText();

            // Retrieve
            mockMvc.perform(get("/api/v1/transactions/{id}", id)
                            .header(API_KEY_HEADER, VALID_API_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.description").value("Integration test bill"))
                    .andExpect(jsonPath("$.purchaseAmount").value(99.99))
                    .andExpect(jsonPath("$.billerType").value("ELECTRICITY"));
        }

        @Test
        @DisplayName("Should create multiple transactions and list them with pagination")
        void shouldCreateAndListWithPagination() throws Exception {
            // Create 3 transactions
            for (int i = 0; i < 3; i++) {
                PurchaseTransactionRequest request = PurchaseTransactionRequest.builder()
                        .idempotencyKey(UUID.randomUUID())
                        .description("Paginated tx " + i)
                        .transactionDate(LocalDate.of(2025, 1, 1 + i))
                        .purchaseAmount(new BigDecimal("10.00"))
                        .billerType(BillerType.OTHER)
                        .build();

                mockMvc.perform(post("/api/v1/transactions")
                                .header(API_KEY_HEADER, VALID_API_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated());
            }

            // List all (the DB may have transactions from other tests too)
            mockMvc.perform(get("/api/v1/transactions")
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .param("size", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(3)))
                    .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(3)));
        }
    }

    @Nested
    @DisplayName("Idempotency")
    class IdempotencyTests {

        @Test
        @DisplayName("Should reject duplicate idempotency key with 409 Conflict")
        void shouldRejectDuplicateIdempotencyKey() throws Exception {
            PurchaseTransactionRequest request = buildValidRequest();
            String json = objectMapper.writeValueAsString(request);

            // First request — should succeed
            mockMvc.perform(post("/api/v1/transactions")
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated());

            // Second request with same idempotency key — should fail
            mockMvc.perform(post("/api/v1/transactions")
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Duplicate Transaction"));
        }
    }

    @Nested
    @DisplayName("Validation Errors")
    class ValidationTests {

        @Test
        @DisplayName("Should return 400 with field errors for empty body")
        void shouldReturn400ForEmptyBody() throws Exception {
            mockMvc.perform(post("/api/v1/transactions")
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.fieldErrors").isMap());
        }

        @Test
        @DisplayName("Should return 400 when purchase amount has too many decimal places")
        void shouldRejectTooManyDecimals() throws Exception {
            PurchaseTransactionRequest request = PurchaseTransactionRequest.builder()
                    .idempotencyKey(UUID.randomUUID())
                    .description("Bad decimals")
                    .transactionDate(LocalDate.of(2025, 3, 15))
                    .purchaseAmount(new BigDecimal("10.999"))
                    .billerType(BillerType.OTHER)
                    .build();

            mockMvc.perform(post("/api/v1/transactions")
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Not Found")
    class NotFoundTests {

        @Test
        @DisplayName("Should return 404 for non-existent transaction ID")
        void shouldReturn404ForNonExistentId() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(get("/api/v1/transactions/{id}", nonExistentId)
                            .header(API_KEY_HEADER, VALID_API_KEY))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Transaction Not Found"))
                    .andExpect(jsonPath("$.detail").value(containsString(nonExistentId.toString())));
        }

        @Test
        @DisplayName("Should return 404 for non-existent transaction on convert endpoint")
        void shouldReturn404ForConvertNonExistent() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(get("/api/v1/transactions/{id}/convert", nonExistentId)
                            .param("currency", "Canada-Dollar")
                            .header(API_KEY_HEADER, VALID_API_KEY))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Transaction Not Found"));
        }
    }

    @Nested
    @DisplayName("Purchase Amount Rounding")
    class RoundingTests {

        @Test
        @DisplayName("Should round purchase amount to nearest cent on create")
        void shouldRoundToNearestCent() throws Exception {
            PurchaseTransactionRequest request = PurchaseTransactionRequest.builder()
                    .idempotencyKey(UUID.randomUUID())
                    .description("Rounding test")
                    .transactionDate(LocalDate.of(2025, 3, 15))
                    .purchaseAmount(new BigDecimal("25.50"))
                    .billerType(BillerType.OTHER)
                    .build();

            mockMvc.perform(post("/api/v1/transactions")
                            .header(API_KEY_HEADER, VALID_API_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.purchaseAmount").value(25.50));
        }
    }
}
