package com.loja.checkout;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CheckoutApplicationTest {
    @Autowired
    ResumoService service;

    @Test
    void contextoSobe() {
        assertNotNull(service);
    }
}
