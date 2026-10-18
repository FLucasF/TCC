package com.loja;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.loja.controller.*;
import com.loja.repository.*;
import com.loja.service.*;
import com.loja.service.club.ClubBenefitsCalculator;
import com.loja.service.coupon.CouponService;
import com.loja.service.insurance.InsuranceCalculator;
import com.loja.service.payment.PaymentAdjustmentCalculator;
import com.loja.service.shipping.ShippingCalculator;
import com.loja.service.shipping.TransLogClient;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class Application {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/loja");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        String transLogUrl = System.getenv().getOrDefault("TRANSLOG_URL", "http://localhost:8081");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        Database database = new Database(dbUrl, dbUser, dbPassword);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        ProductRepository productRepository = new ProductRepository(database);
        CustomerRepository customerRepository = new CustomerRepository(database);
        OrderRepository orderRepository = new OrderRepository(database, objectMapper);
        NotificationRepository notificationRepository = new NotificationRepository(database);

        productRepository.initializeCatalog();

        TransLogClient transLogClient = new TransLogClient(transLogUrl);
        CouponService couponService = new CouponService();
        ShippingCalculator shippingCalculator = new ShippingCalculator(transLogClient);
        InsuranceCalculator insuranceCalculator = new InsuranceCalculator();
        PaymentAdjustmentCalculator paymentAdjustmentCalculator = new PaymentAdjustmentCalculator();
        ClubBenefitsCalculator clubBenefitsCalculator = new ClubBenefitsCalculator();

        CheckoutService checkoutService = new CheckoutService(
            productRepository,
            orderRepository,
            couponService,
            shippingCalculator,
            insuranceCalculator,
            paymentAdjustmentCalculator,
            clubBenefitsCalculator
        );

        OrderService orderService = new OrderService(
            database,
            orderRepository,
            productRepository,
            customerRepository,
            notificationRepository,
            checkoutService
        );

        CheckoutController checkoutController = new CheckoutController(checkoutService);
        OrderController orderController = new OrderController(orderService);
        ProductController productController = new ProductController(productRepository);
        CustomerController customerController = new CustomerController(customerRepository, notificationRepository);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        }).start(port);

        app.post("/checkout/summary", checkoutController::calculateSummary);
        app.post("/orders", orderController::createOrder);
        app.get("/orders/{id}", orderController::getOrder);
        app.post("/orders/{id}/payment", orderController::processPayment);
        app.post("/orders/{id}/shipment", orderController::shipOrder);
        app.post("/orders/{id}/delivery", orderController::deliverOrder);
        app.post("/orders/{id}/cancellation", orderController::cancelOrder);
        app.get("/products/{sku}", productController::getProduct);
        app.get("/customers/{cpf}", customerController::getCustomer);
        app.get("/customers/{cpf}/notifications", customerController::getCustomerNotifications);
        app.post("/notifications/{id}/read", customerController::markNotificationAsRead);

        System.out.println("Server started on port " + port);
    }
}
