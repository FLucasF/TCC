package com.example;

import com.example.api.dto.*;
import com.example.application.service.OrderSummaryCalculator;
import com.example.application.service.ProductService;
import com.example.application.service.TransLogService;
import com.example.domain.entity.OrderSummary;
import com.example.domain.entity.Product;
import com.example.domain.enums.ShippingMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderSummaryCalculatorTest {

    @Mock
    private ProductService productService;

    @Mock
    private TransLogService transLogService;

    private OrderSummaryCalculator calculator;

    @BeforeEach
    public void setUp() {
        calculator = new OrderSummaryCalculator(productService, transLogService);
    }

    @Test
    public void testSimpleCalculation() {
        Product camiseta = new Product("CAM-001", "Camiseta", new BigDecimal("79.90"), new BigDecimal("0.30"), 50, false);
        when(productService.getProduct("CAM-001")).thenReturn(camiseta);

        CheckoutRequestDto request = new CheckoutRequestDto();
        CustomerRequestDto customer = new CustomerRequestDto();
        customer.setCpf("12345678901");
        customer.setEmail("test@test.com");
        request.setCustomer(customer);
        request.setItems(Arrays.asList(new OrderItemDto("CAM-001", 1)));
        request.setZipCode("01310100");
        request.setRegion("SOUTHEAST");
        request.setShippingMethod("STORE_PICKUP");
        request.setPaymentMethod("PIX");
        request.setClubTier("BRONZE");

        OrderSummary summary = calculator.calculate(request);

        assertNotNull(summary);
        assertEquals(new BigDecimal("79.90"), summary.getProductsSubtotal());
    }
}
