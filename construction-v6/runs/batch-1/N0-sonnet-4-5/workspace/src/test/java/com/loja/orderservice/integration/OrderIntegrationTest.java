package com.loja.orderservice.integration;

import com.loja.orderservice.dto.*;
import com.loja.orderservice.entity.Product;
import com.loja.orderservice.exception.BusinessException;
import com.loja.orderservice.model.*;
import com.loja.orderservice.repository.ProductRepository;
import com.loja.orderservice.service.OrderService;
import com.loja.orderservice.service.TransLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
@ActiveProfiles("test")
class OrderIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @MockBean
    private TransLogService transLogService;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        productRepository.save(new Product("CAM-001", "Camiseta básica",
                new BigDecimal("79.90"), new BigDecimal("0.30"), 50, false));
        productRepository.save(new Product("TEN-001", "Tênis corrida",
                new BigDecimal("249.90"), new BigDecimal("1.20"), 10, false));
    }

    @Test
    void testCreateOrderAndPayment() {
        Mockito.when(transLogService.getQuote(anyString(), any(), anyString()))
                .thenReturn(TransLogService.QuoteResult.success(new BigDecimal("33.10"), 2));

        CheckoutRequest request = createRequest(
                "11111111111", "test@example.com",
                List.of(item("CAM-001", 2)),
                "01310100", Region.NORTH, ShippingMethod.EXPRESS,
                null, PaymentMethod.PIX, 1, ClubTier.BRONZE
        );

        OrderResponse order = orderService.createOrder(request);

        assertNotNull(order.getId());
        assertEquals(OrderStatus.AWAITING_PAYMENT, order.getStatus());
        assertNotNull(order.getCreatedAt());
        assertNull(order.getTrackingCode());

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setTransactionId("TX-12345");
        paymentRequest.setPaidAt(Instant.now());

        OrderResponse paidOrder = orderService.processPayment(order.getId(), paymentRequest);

        assertEquals(OrderStatus.PAID, paidOrder.getStatus());

        Product product = productRepository.findById("CAM-001").orElseThrow();
        assertEquals(48, product.getStock());
    }

    @Test
    void testDuplicatePaymentNotification() {
        Mockito.when(transLogService.getQuote(anyString(), any(), anyString()))
                .thenReturn(TransLogService.QuoteResult.success(new BigDecimal("33.10"), 2));

        CheckoutRequest request = createRequest(
                "22222222222", "test@example.com",
                List.of(item("CAM-001", 1)),
                "01310100", Region.SOUTHEAST, ShippingMethod.EXPRESS,
                null, PaymentMethod.PIX, 1, ClubTier.BRONZE
        );

        OrderResponse order = orderService.createOrder(request);

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setTransactionId("TX-UNIQUE");
        paymentRequest.setPaidAt(Instant.now());

        orderService.processPayment(order.getId(), paymentRequest);

        Product product = productRepository.findById("CAM-001").orElseThrow();
        int stockAfterFirstPayment = product.getStock();

        orderService.processPayment(order.getId(), paymentRequest);

        product = productRepository.findById("CAM-001").orElseThrow();
        assertEquals(stockAfterFirstPayment, product.getStock());
    }

    @Test
    void testCancelPaidOrderRestoresStock() {
        Mockito.when(transLogService.getQuote(anyString(), any(), anyString()))
                .thenReturn(TransLogService.QuoteResult.success(new BigDecimal("33.10"), 2));

        CheckoutRequest request = createRequest(
                "33333333333", "test@example.com",
                List.of(item("TEN-001", 2)),
                "01310100", Region.SOUTHEAST, ShippingMethod.EXPRESS,
                null, PaymentMethod.PIX, 1, ClubTier.BRONZE
        );

        OrderResponse order = orderService.createOrder(request);

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setTransactionId("TX-999");
        paymentRequest.setPaidAt(Instant.now());

        orderService.processPayment(order.getId(), paymentRequest);

        Product product = productRepository.findById("TEN-001").orElseThrow();
        assertEquals(8, product.getStock());

        orderService.cancelOrder(order.getId());

        product = productRepository.findById("TEN-001").orElseThrow();
        assertEquals(10, product.getStock());
    }

    @Test
    void testCannotShipUnpaidOrder() {
        Mockito.when(transLogService.getQuote(anyString(), any(), anyString()))
                .thenReturn(TransLogService.QuoteResult.success(new BigDecimal("33.10"), 2));

        CheckoutRequest request = createRequest(
                "44444444444", "test@example.com",
                List.of(item("CAM-001", 1)),
                "01310100", Region.SOUTHEAST, ShippingMethod.EXPRESS,
                null, PaymentMethod.PIX, 1, ClubTier.BRONZE
        );

        OrderResponse order = orderService.createOrder(request);

        ShipmentRequest shipmentRequest = new ShipmentRequest();
        shipmentRequest.setTrackingCode("BR123456789");

        assertThrows(BusinessException.class, () ->
                orderService.shipOrder(order.getId(), shipmentRequest));
    }

    @Test
    void testCannotCancelShippedOrder() {
        Mockito.when(transLogService.getQuote(anyString(), any(), anyString()))
                .thenReturn(TransLogService.QuoteResult.success(new BigDecimal("33.10"), 2));

        CheckoutRequest request = createRequest(
                "55555555555", "test@example.com",
                List.of(item("CAM-001", 1)),
                "01310100", Region.SOUTHEAST, ShippingMethod.EXPRESS,
                null, PaymentMethod.PIX, 1, ClubTier.BRONZE
        );

        OrderResponse order = orderService.createOrder(request);

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setTransactionId("TX-555");
        paymentRequest.setPaidAt(Instant.now());
        orderService.processPayment(order.getId(), paymentRequest);

        ShipmentRequest shipmentRequest = new ShipmentRequest();
        shipmentRequest.setTrackingCode("BR123456789");
        orderService.shipOrder(order.getId(), shipmentRequest);

        assertThrows(BusinessException.class, () -> orderService.cancelOrder(order.getId()));
    }

    @Test
    void testPaymentExpired() {
        Mockito.when(transLogService.getQuote(anyString(), any(), anyString()))
                .thenReturn(TransLogService.QuoteResult.success(new BigDecimal("33.10"), 2));

        CheckoutRequest request = createRequest(
                "66666666666", "test@example.com",
                List.of(item("CAM-001", 1)),
                "01310100", Region.SOUTHEAST, ShippingMethod.EXPRESS,
                null, PaymentMethod.PIX, 1, ClubTier.BRONZE
        );

        OrderResponse order = orderService.createOrder(request);

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setTransactionId("TX-LATE");
        paymentRequest.setPaidAt(order.getCreatedAt().plusSeconds(48 * 3600 + 1));

        assertThrows(BusinessException.class, () ->
                orderService.processPayment(order.getId(), paymentRequest));

        OrderResponse cancelledOrder = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getStatus());
    }

    private CheckoutRequest createRequest(String cpf, String email, List<CheckoutRequest.ItemRequest> items,
                                          String zipCode, Region region, ShippingMethod shippingMethod,
                                          Coupon coupon, PaymentMethod paymentMethod, int installments, ClubTier clubTier) {
        CheckoutRequest request = new CheckoutRequest();
        CheckoutRequest.CustomerInfo customer = new CheckoutRequest.CustomerInfo();
        customer.setCpf(cpf);
        customer.setEmail(email);
        request.setCustomer(customer);
        request.setItems(items);
        request.setZipCode(zipCode);
        request.setRegion(region);
        request.setShippingMethod(shippingMethod);
        request.setCoupon(coupon);
        request.setPaymentMethod(paymentMethod);
        request.setInstallments(installments);
        request.setClubTier(clubTier);
        return request;
    }

    private CheckoutRequest.ItemRequest item(String sku, int quantity) {
        CheckoutRequest.ItemRequest item = new CheckoutRequest.ItemRequest();
        item.setSku(sku);
        item.setQuantity(quantity);
        return item;
    }
}
