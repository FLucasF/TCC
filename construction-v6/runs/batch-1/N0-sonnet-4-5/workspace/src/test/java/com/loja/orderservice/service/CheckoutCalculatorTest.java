package com.loja.orderservice.service;

import com.loja.orderservice.dto.CheckoutRequest;
import com.loja.orderservice.entity.OrderSummary;
import com.loja.orderservice.entity.Product;
import com.loja.orderservice.model.*;
import com.loja.orderservice.repository.OrderRepository;
import com.loja.orderservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
@ActiveProfiles("test")
class CheckoutCalculatorTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private TransLogService transLogService;

    @Autowired
    private CheckoutCalculator checkoutCalculator;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        orderRepository.deleteAll();

        productRepository.save(new Product("CAM-001", "Camiseta básica",
                new BigDecimal("79.90"), new BigDecimal("0.30"), 50, false));
        productRepository.save(new Product("TEN-001", "Tênis corrida",
                new BigDecimal("249.90"), new BigDecimal("1.20"), 10, false));
        productRepository.save(new Product("FON-001", "Fone sem fio",
                new BigDecimal("199.90"), new BigDecimal("0.25"), 5, false));
        productRepository.save(new Product("MEI-001", "Meia esportiva",
                new BigDecimal("19.90"), new BigDecimal("0.10"), 100, false));
        productRepository.save(new Product("JAQ-001", "Jaqueta corta-vento",
                new BigDecimal("389.90"), new BigDecimal("0.80"), 3, false));
    }

    @Test
    void testExample1() {
        Mockito.when(transLogService.getQuote(anyString(), any(), anyString()))
                .thenReturn(TransLogService.QuoteResult.success(new BigDecimal("33.10"), 2));

        CheckoutRequest request = createRequest(
                "12345678909", "ana@exemplo.com",
                List.of(
                        item("CAM-001", 2),
                        item("TEN-001", 1)
                ),
                "01310100", Region.NORTH, ShippingMethod.EXPRESS,
                Coupon.WELCOME10, PaymentMethod.PIX, 1, ClubTier.BRONZE
        );

        OrderSummary summary = checkoutCalculator.calculate(request);

        assertEquals(new BigDecimal("409.70"), summary.getProductsSubtotal());
        assertEquals(new BigDecimal("40.97"), summary.getCouponDiscount());
        assertEquals(new BigDecimal("33.10"), summary.getShippingCost());
        assertEquals(5, summary.getDeliveryDays());
        assertEquals(new BigDecimal("10.24"), summary.getInsurance());
        assertEquals(new BigDecimal("0.00"), summary.getPaymentAdjustment());
        assertEquals(new BigDecimal("412.07"), summary.getFinalTotal());
        assertEquals(1, summary.getInstallments());
        assertEquals(new BigDecimal("412.07"), summary.getInstallmentAmount());
        assertEquals(new BigDecimal("0.00"), summary.getNextPurchaseCredit());
        assertFalse(summary.getGift());
    }

    @Test
    void testExample2() {
        Mockito.when(transLogService.getQuote(anyString(), any(), anyString()))
                .thenReturn(TransLogService.QuoteResult.success(new BigDecimal("15.60"), 7));

        CheckoutRequest request = createRequest(
                "12345678909", "ana@exemplo.com",
                List.of(
                        item("CAM-001", 2),
                        item("TEN-001", 1)
                ),
                "01310100", Region.MIDWEST, ShippingMethod.ECONOMY,
                null, PaymentMethod.CREDIT_CARD, 6, ClubTier.SILVER
        );

        OrderSummary summary = checkoutCalculator.calculate(request);

        assertEquals(new BigDecimal("409.70"), summary.getProductsSubtotal());
        assertEquals(new BigDecimal("0.00"), summary.getCouponDiscount());
        assertEquals(new BigDecimal("15.60"), summary.getShippingCost());
        assertEquals(7, summary.getDeliveryDays());
        assertEquals(new BigDecimal("6.15"), summary.getInsurance());
        assertEquals(new BigDecimal("30.55"), summary.getPaymentAdjustment());
        assertEquals(new BigDecimal("462.00"), summary.getFinalTotal());
        assertEquals(6, summary.getInstallments());
        assertEquals(new BigDecimal("77.00"), summary.getInstallmentAmount());
        assertEquals(new BigDecimal("8.19"), summary.getNextPurchaseCredit());
        assertFalse(summary.getGift());
    }

    @Test
    void testExample3() {
        CheckoutRequest request = createRequest(
                "12345678909", "ana@exemplo.com",
                List.of(item("FON-001", 2)),
                "01310100", Region.NORTHEAST, ShippingMethod.COURIER,
                Coupon.MINUS50, PaymentMethod.BANK_SLIP, 1, ClubTier.GOLD
        );

        OrderSummary summary = checkoutCalculator.calculate(request);

        assertEquals(new BigDecimal("399.80"), summary.getProductsSubtotal());
        assertEquals(new BigDecimal("50.00"), summary.getCouponDiscount());
        assertEquals(new BigDecimal("18.00"), summary.getShippingCost());
        assertEquals(0, summary.getDeliveryDays());
        assertEquals(new BigDecimal("8.00"), summary.getInsurance());
        assertEquals(new BigDecimal("3.49"), summary.getPaymentAdjustment());
        assertEquals(new BigDecimal("379.29"), summary.getFinalTotal());
        assertEquals(1, summary.getInstallments());
        assertEquals(new BigDecimal("379.29"), summary.getInstallmentAmount());
        assertEquals(new BigDecimal("19.99"), summary.getNextPurchaseCredit());
        assertFalse(summary.getGift());
    }

    @Test
    void testExample4() {
        CheckoutRequest request = createRequest(
                "12345678909", "ana@exemplo.com",
                List.of(
                        item("MEI-001", 7),
                        item("CAM-001", 2)
                ),
                "01310100", Region.SOUTH, ShippingMethod.STORE_PICKUP,
                Coupon.BUY3PAY2, PaymentMethod.CREDIT_CARD, 3, ClubTier.SILVER
        );

        OrderSummary summary = checkoutCalculator.calculate(request);

        assertEquals(new BigDecimal("299.10"), summary.getProductsSubtotal());
        assertEquals(new BigDecimal("39.80"), summary.getCouponDiscount());
        assertEquals(new BigDecimal("0.00"), summary.getShippingCost());
        assertEquals(1, summary.getDeliveryDays());
        assertEquals(new BigDecimal("2.99"), summary.getInsurance());
        assertEquals(new BigDecimal("0.00"), summary.getPaymentAdjustment());
        assertEquals(new BigDecimal("262.29"), summary.getFinalTotal());
        assertEquals(3, summary.getInstallments());
        assertEquals(new BigDecimal("87.43"), summary.getInstallmentAmount());
        assertEquals(new BigDecimal("5.98"), summary.getNextPurchaseCredit());
        assertFalse(summary.getGift());
    }

    @Test
    void testExample5() {
        Mockito.when(transLogService.getQuote(anyString(), any(), anyString()))
                .thenReturn(TransLogService.QuoteResult.success(new BigDecimal("24.75"), 2));

        CheckoutRequest request = createRequest(
                "12345678909", "ana@exemplo.com",
                List.of(item("CAM-001", 3)),
                "01310100", Region.SOUTHEAST, ShippingMethod.EXPRESS,
                Coupon.BUY3PAY2, PaymentMethod.PIX, 1, ClubTier.BRONZE
        );

        OrderSummary summary = checkoutCalculator.calculate(request);

        assertEquals(new BigDecimal("239.70"), summary.getProductsSubtotal());
        assertEquals(new BigDecimal("71.91"), summary.getCouponDiscount());
        assertEquals(new BigDecimal("24.75"), summary.getShippingCost());
        assertEquals(2, summary.getDeliveryDays());
        assertEquals(new BigDecimal("2.40"), summary.getInsurance());
        assertEquals(new BigDecimal("0.00"), summary.getPaymentAdjustment());
        assertEquals(new BigDecimal("194.94"), summary.getFinalTotal());
        assertEquals(1, summary.getInstallments());
        assertEquals(new BigDecimal("194.94"), summary.getInstallmentAmount());
        assertEquals(new BigDecimal("0.00"), summary.getNextPurchaseCredit());
        assertFalse(summary.getGift());
    }

    @Test
    void testExample6() {
        Mockito.when(transLogService.getQuote(anyString(), any(), anyString()))
                .thenReturn(TransLogService.QuoteResult.success(new BigDecimal("22.10"), 7));

        CheckoutRequest request = createRequest(
                "12345678909", "ana@exemplo.com",
                List.of(item("MEI-001", 1)),
                "01310100", Region.SOUTH, ShippingMethod.ECONOMY,
                Coupon.FREESHIPPING, PaymentMethod.BANK_SLIP, 1, ClubTier.BRONZE
        );

        OrderSummary summary = checkoutCalculator.calculate(request);

        assertEquals(new BigDecimal("19.90"), summary.getProductsSubtotal());
        assertEquals(new BigDecimal("5.97"), summary.getCouponDiscount());
        assertEquals(new BigDecimal("22.10"), summary.getShippingCost());
        assertEquals(7, summary.getDeliveryDays());
        assertEquals(new BigDecimal("0.20"), summary.getInsurance());
        assertEquals(new BigDecimal("3.49"), summary.getPaymentAdjustment());
        assertEquals(new BigDecimal("39.72"), summary.getFinalTotal());
        assertEquals(1, summary.getInstallments());
        assertEquals(new BigDecimal("39.72"), summary.getInstallmentAmount());
        assertEquals(new BigDecimal("0.00"), summary.getNextPurchaseCredit());
        assertFalse(summary.getGift());
    }

    @Test
    void testExample7() {
        Mockito.when(transLogService.getQuote(anyString(), any(), anyString()))
                .thenReturn(TransLogService.QuoteResult.success(new BigDecimal("50.80"), 3));

        CheckoutRequest request = createRequest(
                "12345678909", "ana@exemplo.com",
                List.of(
                        item("JAQ-001", 3),
                        item("TEN-001", 1)
                ),
                "01310100", Region.NORTH, ShippingMethod.EXPRESS,
                null, PaymentMethod.PIX, 1, ClubTier.GOLD
        );

        OrderSummary summary = checkoutCalculator.calculate(request);

        assertEquals(new BigDecimal("1419.60"), summary.getProductsSubtotal());
        assertEquals(new BigDecimal("0.00"), summary.getCouponDiscount());
        assertEquals(new BigDecimal("0.00"), summary.getShippingCost());
        assertEquals(6, summary.getDeliveryDays());
        assertEquals(new BigDecimal("30.00"), summary.getInsurance());
        assertEquals(new BigDecimal("-72.48"), summary.getPaymentAdjustment());
        assertEquals(new BigDecimal("1377.12"), summary.getFinalTotal());
        assertEquals(1, summary.getInstallments());
        assertEquals(new BigDecimal("1377.12"), summary.getInstallmentAmount());
        assertEquals(new BigDecimal("70.98"), summary.getNextPurchaseCredit());
        assertTrue(summary.getGift());
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
