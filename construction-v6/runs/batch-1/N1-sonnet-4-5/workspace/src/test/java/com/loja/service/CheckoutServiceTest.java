package com.loja.service;

import com.loja.domain.*;
import com.loja.dto.OrderRequest;
import com.loja.integration.TransLogClient;
import com.loja.repository.OrderRepository;
import com.loja.repository.ProductRepository;
import com.loja.service.club.BronzeClub;
import com.loja.service.club.ClubTierStrategy;
import com.loja.service.club.GoldClub;
import com.loja.service.club.SilverClub;
import com.loja.service.coupon.*;
import com.loja.service.payment.BankSlipPayment;
import com.loja.service.payment.CreditCardPayment;
import com.loja.service.payment.PaymentStrategy;
import com.loja.service.payment.PixPayment;
import com.loja.service.shipping.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class CheckoutServiceTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private TransLogClient transLogClient;

    private CheckoutService checkoutService;

    @BeforeEach
    void setUp() {
        transLogClient = mock(TransLogClient.class);

        InsuranceService insuranceService = new InsuranceService();

        List<CouponStrategy> coupons = List.of(
            new Welcome10Coupon(),
            new Minus50Coupon(),
            new FreeShippingCoupon(),
            new Buy3Pay2Coupon()
        );

        List<ShippingStrategy> shippings = List.of(
            new StorePickupShipping(),
            new CourierShipping(),
            new EconomyShipping(transLogClient),
            new ExpressShipping(transLogClient)
        );

        List<PaymentStrategy> payments = List.of(
            new PixPayment(),
            new CreditCardPayment(),
            new BankSlipPayment()
        );

        List<ClubTierStrategy> clubs = List.of(
            new BronzeClub(),
            new SilverClub(),
            new GoldClub()
        );

        checkoutService = new CheckoutService(
            productRepository, orderRepository, insuranceService,
            coupons, shippings, payments, clubs
        );

        setupProducts();
    }

    private void setupProducts() {
        productRepository.deleteAll();
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
    void example1() {
        when(transLogClient.getQuote("01310100", 1800, "EXP"))
            .thenReturn(new TransLogClient.QuoteResult(3310, 41));

        OrderRequest request = new OrderRequest(
            new OrderRequest.CustomerInfo("12345678909", "ana@exemplo.com"),
            List.of(
                new OrderRequest.ItemInfo("CAM-001", 2),
                new OrderRequest.ItemInfo("TEN-001", 1)
            ),
            "01310100",
            Region.NORTH,
            ShippingMethod.EXPRESS,
            CouponCode.WELCOME10,
            PaymentMethod.PIX,
            1,
            ClubTier.BRONZE
        );

        OrderSummary summary = checkoutService.calculateSummary(request);

        assertEquals(new BigDecimal("409.70"), summary.productsSubtotal().value());
        assertEquals(new BigDecimal("40.97"), summary.couponDiscount().value());
        assertEquals(new BigDecimal("33.10"), summary.shippingCost().value());
        assertEquals(5, summary.deliveryDays());
        assertEquals(new BigDecimal("10.24"), summary.insurance().value());
        assertEquals(new BigDecimal("0.00"), summary.paymentAdjustment().value());
        assertEquals(new BigDecimal("412.07"), summary.finalTotal().value());
        assertEquals(1, summary.installments());
        assertEquals(new BigDecimal("412.07"), summary.installmentAmount().value());
        assertEquals(new BigDecimal("0.00"), summary.nextPurchaseCredit().value());
        assertFalse(summary.gift());
    }

    @Test
    void example2() {
        when(transLogClient.getQuote("01310100", 1800, "STD"))
            .thenReturn(new TransLogClient.QuoteResult(1560, 168));

        OrderRequest request = new OrderRequest(
            new OrderRequest.CustomerInfo("12345678909", "ana@exemplo.com"),
            List.of(
                new OrderRequest.ItemInfo("CAM-001", 2),
                new OrderRequest.ItemInfo("TEN-001", 1)
            ),
            "01310100",
            Region.MIDWEST,
            ShippingMethod.ECONOMY,
            null,
            PaymentMethod.CREDIT_CARD,
            6,
            ClubTier.SILVER
        );

        OrderSummary summary = checkoutService.calculateSummary(request);

        assertEquals(new BigDecimal("409.70"), summary.productsSubtotal().value());
        assertEquals(new BigDecimal("0.00"), summary.couponDiscount().value());
        assertEquals(new BigDecimal("15.60"), summary.shippingCost().value());
        assertEquals(7, summary.deliveryDays());
        assertEquals(new BigDecimal("6.15"), summary.insurance().value());
        assertEquals(new BigDecimal("30.55"), summary.paymentAdjustment().value());
        assertEquals(new BigDecimal("462.00"), summary.finalTotal().value());
        assertEquals(6, summary.installments());
        assertEquals(new BigDecimal("77.00"), summary.installmentAmount().value());
        assertEquals(new BigDecimal("8.19"), summary.nextPurchaseCredit().value());
        assertFalse(summary.gift());
    }

    @Test
    void example3() {
        OrderRequest request = new OrderRequest(
            new OrderRequest.CustomerInfo("12345678909", "ana@exemplo.com"),
            List.of(
                new OrderRequest.ItemInfo("FON-001", 2)
            ),
            "01310100",
            Region.NORTHEAST,
            ShippingMethod.COURIER,
            CouponCode.MINUS50,
            PaymentMethod.BANK_SLIP,
            1,
            ClubTier.GOLD
        );

        OrderSummary summary = checkoutService.calculateSummary(request);

        assertEquals(new BigDecimal("399.80"), summary.productsSubtotal().value());
        assertEquals(new BigDecimal("50.00"), summary.couponDiscount().value());
        assertEquals(new BigDecimal("18.00"), summary.shippingCost().value());
        assertEquals(0, summary.deliveryDays());
        assertEquals(new BigDecimal("8.00"), summary.insurance().value());
        assertEquals(new BigDecimal("3.49"), summary.paymentAdjustment().value());
        assertEquals(new BigDecimal("379.29"), summary.finalTotal().value());
        assertEquals(1, summary.installments());
        assertEquals(new BigDecimal("379.29"), summary.installmentAmount().value());
        assertEquals(new BigDecimal("19.99"), summary.nextPurchaseCredit().value());
        assertFalse(summary.gift());
    }
}
