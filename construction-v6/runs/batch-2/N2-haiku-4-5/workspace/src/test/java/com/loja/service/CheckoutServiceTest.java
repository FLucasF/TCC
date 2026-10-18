package com.loja.service;

import com.loja.model.*;
import com.loja.repository.*;
import com.loja.strategy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.*;

public class CheckoutServiceTest {
    private CheckoutService checkoutService;
    private ProductRepository productRepo;
    private OrderRepository orderRepo;

    @BeforeEach
    public void setUp() {
        productRepo = new MockProductRepository();
        orderRepo = new MockOrderRepository();
        OrderService orderService = new OrderService(productRepo, orderRepo,
            new MockCustomerRepository(), new MockNotificationRepository());
        checkoutService = new CheckoutService(productRepo, orderService);
    }

    @Test
    public void testBasicCheckout() throws CheckoutException {
        CheckoutRequest request = new CheckoutRequest();
        request.customer = new CustomerRequest("12345678909", "ana@example.com");
        request.items = Arrays.asList(new OrderItem("CAM-001", 2));
        request.zipCode = "01310100";
        request.region = Region.SOUTHEAST;
        request.shippingMethod = ShippingMethod.STORE_PICKUP;
        request.paymentMethod = PaymentMethod.PIX;
        request.clubTier = ClubTier.BRONZE;

        OrderSummary summary = checkoutService.calculateSummary(request);

        assertThat(summary.productsSubtotal).isEqualTo(159.80);
        assertThat(summary.couponDiscount).isEqualTo(0.0);
        assertThat(summary.shippingCost).isEqualTo(0.0);
    }


    static class MockProductRepository extends ProductRepository {
        private final Map<String, Product> products = initializeProducts();

        public MockProductRepository() {
            super(null);
        }

        private Map<String, Product> initializeProducts() {
            Map<String, Product> map = new HashMap<>();
            map.put("CAM-001", new Product("CAM-001", "Camiseta básica", 79.90, 0.30, 50, false));
            map.put("TEN-001", new Product("TEN-001", "Tênis corrida", 249.90, 1.20, 10, false));
            map.put("JAQ-001", new Product("JAQ-001", "Jaqueta corta-vento", 389.90, 0.80, 3, false));
            map.put("REL-001", new Product("REL-001", "Relógio digital", 129.90, 0.20, 8, true));
            return map;
        }

        @Override
        public Product findBySku(String sku) {
            return products.get(sku);
        }
    }

    static class MockOrderRepository extends OrderRepository {
        public MockOrderRepository() {
            super(null);
        }

        @Override
        public Order findById(String id) {
            return null;
        }

        @Override
        public boolean hasCompletedOrder(String cpf) {
            return false;
        }
    }

    static class MockCustomerRepository extends CustomerRepository {
        public MockCustomerRepository() {
            super(null);
        }

        @Override
        public Customer findByCpf(String cpf) {
            return null;
        }

        @Override
        public void createOrUpdate(String cpf, Double creditBalance) {}
    }

    static class MockNotificationRepository extends NotificationRepository {
        public MockNotificationRepository() {
            super(null);
        }

        @Override
        public void create(Notification notification) {}
    }
}
