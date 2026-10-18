package com.loja;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.loja.domain.*;
import com.loja.repository.*;
import com.loja.service.*;
import com.loja.service.club.ClubBenefitsCalculator;
import com.loja.service.coupon.CouponService;
import com.loja.service.insurance.InsuranceCalculator;
import com.loja.service.payment.PaymentAdjustmentCalculator;
import com.loja.service.shipping.ShippingCalculator;
import com.loja.service.shipping.ShippingResult;
import com.loja.service.shipping.TransLogClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutServiceTest {
    private Database database;
    private CheckoutService checkoutService;
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        database = new Database("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL", "sa", "");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        productRepository = new ProductRepository(database);
        CustomerRepository customerRepository = new CustomerRepository(database);
        OrderRepository orderRepository = new OrderRepository(database, objectMapper);

        productRepository.initializeCatalog();

        TransLogClient transLogClient = new MockTransLogClient();
        CouponService couponService = new CouponService();
        ShippingCalculator shippingCalculator = new ShippingCalculator(transLogClient);
        InsuranceCalculator insuranceCalculator = new InsuranceCalculator();
        PaymentAdjustmentCalculator paymentAdjustmentCalculator = new PaymentAdjustmentCalculator();
        ClubBenefitsCalculator clubBenefitsCalculator = new ClubBenefitsCalculator();

        checkoutService = new CheckoutService(
            productRepository,
            orderRepository,
            couponService,
            shippingCalculator,
            insuranceCalculator,
            paymentAdjustmentCalculator,
            clubBenefitsCalculator
        );
    }

    @Test
    void testExample1() throws Exception {
        CheckoutRequest request = new CheckoutRequest();
        CheckoutRequest.CustomerInfo customer = new CheckoutRequest.CustomerInfo();
        customer.setCpf("12345678909");
        customer.setEmail("ana@exemplo.com");
        request.setCustomer(customer);

        List<OrderItem> items = List.of(
            new OrderItem("CAM-001", 2),
            new OrderItem("TEN-001", 1)
        );
        request.setItems(items);
        request.setZipCode("01310100");
        request.setRegion(Region.NORTH);
        request.setShippingMethod(ShippingMethod.EXPRESS);
        request.setCoupon("WELCOME10");
        request.setPaymentMethod(PaymentMethod.PIX);
        request.setClubTier(ClubTier.BRONZE);

        CheckoutSummary summary = checkoutService.calculateSummary(request);

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
        assertFalse(summary.isGift());
    }

    static class MockTransLogClient extends TransLogClient {
        public MockTransLogClient() {
            super("http://mock");
        }

        @Override
        public ShippingResult quote(String zipCode, BigDecimal weightKg, ShippingMethod method, Region region) {
            if (method == ShippingMethod.EXPRESS) {
                return new ShippingResult(new BigDecimal("33.10"), 2 + (region == Region.NORTH ? 3 : 0));
            }
            return new ShippingResult(new BigDecimal("15.60"), 7);
        }
    }
}
