package com.clothing.service;

import com.clothing.model.*;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class CheckoutServiceTest {

    private ProductRepository mockProductRepo;
    private CustomerRepository mockCustomerRepo;
    private OrderRepository mockOrderRepo;
    private CheckoutService checkoutService;

    @Before
    public void setUp() {
        mockProductRepo = new ProductRepository();
        mockCustomerRepo = new CustomerRepository();
        mockOrderRepo = new OrderRepository();
        checkoutService = new CheckoutService(mockProductRepo, mockCustomerRepo, mockOrderRepo);
    }

    @Test
    public void testMoneyRounding() {
        // Test HALF_EVEN rounding
        assertEquals(new BigDecimal("3.00"), Money.round(new BigDecimal("2.995")));
        assertEquals(new BigDecimal("2.98"), Money.round(new BigDecimal("2.985")));
        assertEquals(new BigDecimal("2.00"), Money.round(new BigDecimal("2.004")));
        assertEquals(new BigDecimal("2.00"), Money.round(new BigDecimal("1.996")));
    }
}
