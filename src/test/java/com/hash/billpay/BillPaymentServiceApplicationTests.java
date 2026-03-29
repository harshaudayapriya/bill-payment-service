package com.hash.billpay;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BillPaymentsApplicationTests {

    @Test
    void contextLoads() {
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}