package com.loja.orderservice.service;

import com.loja.orderservice.domain.entity.Product;
import com.loja.orderservice.domain.enums.*;
import com.loja.orderservice.dto.CheckoutSummary;
import com.loja.orderservice.dto.CheckoutSummaryRequest;
import com.loja.orderservice.exception.OrderServiceException;
import com.loja.orderservice.integration.TransLogClient;
import com.loja.orderservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private TransLogClient transLogClient;

    private CheckoutService checkoutService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        checkoutService = new CheckoutService(productRepository, transLogClient, objectMapper);
    }

    @Test
    void testCalculateSummaryWithEmptyCart() {
        CheckoutSummaryRequest request = new CheckoutSummaryRequest();
        request.setItems(List.of());
        request.setCustomer(new CheckoutSummaryRequest.CustomerInfo());
        request.getCustomer().setCpf("12345678901");
        request.getCustomer().setEmail("test@test.com");

        assertThrows(OrderServiceException.class, () -> checkoutService.calculateSummary(request));
    }

    @Test
    void testCalculateSummaryWithValidRequest() {
        Product product = Product.builder()
                .sku("CAM-001")
                .name("Camiseta básica")
                .price(new BigDecimal("79.90"))
                .weightKg(new BigDecimal("0.30"))
                .stock(50L)
                .discontinued(false)
                .build();

        when(productRepository.findById("CAM-001")).thenReturn(Optional.of(product));

        CheckoutSummaryRequest request = new CheckoutSummaryRequest();
        request.setItems(List.of(
                createOrderItem("CAM-001", 1L)
        ));
        request.setCustomer(createCustomer("12345678901", "test@test.com"));
        request.setZipCode("01310100");
        request.setRegion(Region.SOUTHEAST);
        request.setShippingMethod(ShippingMethod.STORE_PICKUP);
        request.setPaymentMethod(PaymentMethod.PIX);
        request.setClubTier(ClubTier.BRONZE);

        CheckoutSummary summary = checkoutService.calculateSummary(request);

        assertNotNull(summary);
        assertEquals(0, summary.getProductsSubtotal().compareTo(new BigDecimal("79.90")));
        assertEquals(0, summary.getCouponDiscount().compareTo(BigDecimal.ZERO));
        assertEquals(0, summary.getShippingCost().compareTo(BigDecimal.ZERO));
        assertEquals(1, summary.getDeliveryDays());
        assertTrue(summary.getFinalTotal().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testCalculateSummaryWithDiscontinuedProduct() {
        Product product = Product.builder()
                .sku("REL-001")
                .name("Relógio digital")
                .price(new BigDecimal("129.90"))
                .weightKg(new BigDecimal("0.20"))
                .stock(8L)
                .discontinued(true)
                .build();

        when(productRepository.findById("REL-001")).thenReturn(Optional.of(product));

        CheckoutSummaryRequest request = new CheckoutSummaryRequest();
        request.setItems(List.of(createOrderItem("REL-001", 1L)));
        request.setCustomer(createCustomer("12345678901", "test@test.com"));
        request.setZipCode("01310100");
        request.setRegion(Region.SOUTHEAST);
        request.setShippingMethod(ShippingMethod.STORE_PICKUP);
        request.setPaymentMethod(PaymentMethod.PIX);
        request.setClubTier(ClubTier.BRONZE);

        assertThrows(OrderServiceException.class, () -> checkoutService.calculateSummary(request));
    }

    @Test
    void testCalculateSummaryWithCourierTooHeavy() {
        Product product = Product.builder()
                .sku("TEN-001")
                .name("Tênis corrida")
                .price(new BigDecimal("249.90"))
                .weightKg(new BigDecimal("1.20"))
                .stock(10L)
                .discontinued(false)
                .build();

        when(productRepository.findById("TEN-001")).thenReturn(Optional.of(product));

        CheckoutSummaryRequest request = new CheckoutSummaryRequest();
        request.setItems(List.of(createOrderItem("TEN-001", 5L)));
        request.setCustomer(createCustomer("12345678901", "test@test.com"));
        request.setZipCode("01310100");
        request.setRegion(Region.SOUTHEAST);
        request.setShippingMethod(ShippingMethod.COURIER);
        request.setPaymentMethod(PaymentMethod.PIX);
        request.setClubTier(ClubTier.BRONZE);

        assertThrows(OrderServiceException.class, () -> checkoutService.calculateSummary(request));
    }

    private CheckoutSummaryRequest.OrderItemRequest createOrderItem(String sku, Long quantity) {
        CheckoutSummaryRequest.OrderItemRequest item = new CheckoutSummaryRequest.OrderItemRequest();
        item.setSku(sku);
        item.setQuantity(quantity);
        return item;
    }

    private CheckoutSummaryRequest.CustomerInfo createCustomer(String cpf, String email) {
        CheckoutSummaryRequest.CustomerInfo customer = new CheckoutSummaryRequest.CustomerInfo();
        customer.setCpf(cpf);
        customer.setEmail(email);
        return customer;
    }
}
