package com.hash.billpay.config;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying the security configuration
 */
@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorSecurityTest {


    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Actuator Security — HTTP Basic Auth")
    class ActuatorEndpoints {

        @Test
        @DisplayName("GET /actuator/health should return 200 with valid credentials")
        void healthEndpointShouldBeAccessibleWithAuth() throws Exception {
            mockMvc.perform(get("/actuator/health")
                            .with(httpBasic("test-admin", "test-password")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }

        @Test
        @DisplayName("GET /actuator/metrics should return 401 without credentials")
        void metricsEndpointShouldRequireAuth() throws Exception {
            mockMvc.perform(get("/actuator/metrics"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /actuator/env should return 401 without credentials")
        void envEndpointShouldRequireAuth() throws Exception {
            mockMvc.perform(get("/actuator/env"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /actuator/info should return 401 without credentials")
        void infoEndpointShouldRequireAuth() throws Exception {
            mockMvc.perform(get("/actuator/info"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /actuator/metrics should return 200 with valid credentials")
        void metricsEndpointShouldBeAccessibleWithAuth() throws Exception {
            mockMvc.perform(get("/actuator/metrics")
                            .with(httpBasic("test-admin", "test-password")))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /actuator/env should return 200 with valid credentials")
        void envEndpointShouldBeAccessibleWithAuth() throws Exception {
            mockMvc.perform(get("/actuator/env")
                            .with(httpBasic("test-admin", "test-password")))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /actuator/metrics should return 401 with wrong credentials")
        void metricsEndpointShouldRejectBadCredentials() throws Exception {
            mockMvc.perform(get("/actuator/metrics")
                            .with(httpBasic("wrong-user", "wrong-password")))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Public Endpoints — No Auth")
    class PublicEndpoints {

        @Test
        @DisplayName("GET /swagger-ui.html should redirect without authentication")
        void swaggerUiShouldBePublic() throws Exception {
            mockMvc.perform(get("/swagger-ui.html"))
                    .andExpect(status().is3xxRedirection());
        }
    }
}
