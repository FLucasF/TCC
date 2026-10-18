package com.loja;

import com.loja.api.controllers.ApiController;
import com.loja.domain.entities.*;
import com.loja.domain.observers.CreditObserver;
import com.loja.domain.observers.NotificationObserver;
import com.loja.domain.observers.PaymentConfirmedObserver;
import com.loja.domain.observers.StockObserver;
import com.loja.domain.strategies.club.*;
import com.loja.domain.strategies.coupon.*;
import com.loja.domain.strategies.payment.*;
import com.loja.domain.strategies.shipping.*;
import com.loja.infrastructure.Database;
import com.loja.infrastructure.TransLogClient;
import com.loja.repositories.*;
import com.loja.repositories.impl.*;
import com.loja.services.CheckoutService;
import com.loja.services.OrderService;
import com.loja.services.ValidationService;
import io.javalin.Javalin;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/loja");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        String transLogUrl = System.getenv().getOrDefault("TRANSLOG_URL", "http://localhost:8081");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        Database database = new Database(dbUrl, dbUser, dbPassword);
        database.initialize();

        ProductRepository productRepository = new ProductRepositoryImpl(database);
        productRepository.initializeCatalog();

        OrderRepository orderRepository = new OrderRepositoryImpl(database);
        CustomerRepository customerRepository = new CustomerRepositoryImpl(database);
        NotificationRepository notificationRepository = new NotificationRepositoryImpl(database);

        TransLogClient transLogClient = new TransLogClient(transLogUrl);

        Map<ShippingMethod, ShippingStrategy> shippingStrategies = Map.of(
                ShippingMethod.ECONOMY, new EconomyShipping(transLogClient),
                ShippingMethod.EXPRESS, new ExpressShipping(transLogClient),
                ShippingMethod.STORE_PICKUP, new StorePickupShipping(),
                ShippingMethod.COURIER, new CourierShipping()
        );

        Map<CouponCode, CouponStrategy> couponStrategies = Map.of(
                CouponCode.WELCOME10, new Welcome10Coupon(orderRepository),
                CouponCode.MINUS50, new Minus50Coupon(),
                CouponCode.FREESHIPPING, new FreeShippingCoupon(),
                CouponCode.BUY3PAY2, new Buy3Pay2Coupon(productRepository)
        );

        Map<ClubTier, ClubTierStrategy> clubTierStrategies = Map.of(
                ClubTier.BRONZE, new BronzeTier(),
                ClubTier.SILVER, new SilverTier(),
                ClubTier.GOLD, new GoldTier()
        );

        Map<PaymentMethod, PaymentMethodStrategy> paymentMethodStrategies = Map.of(
                PaymentMethod.PIX, new PixPayment(),
                PaymentMethod.CREDIT_CARD, new CreditCardPayment(),
                PaymentMethod.BANK_SLIP, new BankSlipPayment()
        );

        List<PaymentConfirmedObserver> paymentObservers = List.of(
                new StockObserver(productRepository),
                new CreditObserver(customerRepository),
                new NotificationObserver(notificationRepository)
        );

        CheckoutService checkoutService = new CheckoutService(
                productRepository,
                shippingStrategies,
                couponStrategies,
                clubTierStrategies,
                paymentMethodStrategies
        );

        OrderService orderService = new OrderService(
                orderRepository,
                customerRepository,
                notificationRepository,
                productRepository,
                paymentObservers
        );

        ValidationService validationService = new ValidationService(
                productRepository,
                orderRepository,
                shippingStrategies,
                couponStrategies,
                paymentMethodStrategies
        );

        ApiController apiController = new ApiController(
                checkoutService,
                orderService,
                validationService,
                productRepository,
                customerRepository,
                notificationRepository,
                orderRepository
        );

        Javalin app = Javalin.create().start(port);
        apiController.registerRoutes(app);

        System.out.println("Server started on port " + port);
    }
}
