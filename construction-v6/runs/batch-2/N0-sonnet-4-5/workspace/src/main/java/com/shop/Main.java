package com.shop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shop.config.Database;
import com.shop.controller.*;
import com.shop.repository.*;
import com.shop.service.*;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class Main {
    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/shop");
        String dbUser = System.getenv().getOrDefault("DB_USER", "postgres");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "postgres");
        String translogUrl = System.getenv().getOrDefault("TRANSLOG_URL", "http://localhost:8081");
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        Database database = new Database(dbUrl, dbUser, dbPassword);
        database.initialize();

        ProductRepository productRepository = new ProductRepository(database);
        CustomerRepository customerRepository = new CustomerRepository(database);
        OrderRepository orderRepository = new OrderRepository(database);
        NotificationRepository notificationRepository = new NotificationRepository(database);

        ShippingService shippingService = new ShippingService(translogUrl);
        CouponService couponService = new CouponService();
        ClubService clubService = new ClubService();
        PaymentService paymentService = new PaymentService();

        CheckoutService checkoutService = new CheckoutService(
                productRepository, orderRepository, shippingService,
                couponService, clubService, paymentService);

        OrderService orderService = new OrderService(
                database, orderRepository, productRepository,
                customerRepository, notificationRepository, checkoutService);

        CheckoutController checkoutController = new CheckoutController(checkoutService);
        OrderController orderController = new OrderController(orderService);
        ProductController productController = new ProductController(productRepository);
        CustomerController customerController = new CustomerController(customerRepository, notificationRepository);

        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, true));
        });

        app.post("/checkout/summary", checkoutController::calculateSummary);

        app.post("/orders", orderController::createOrder);
        app.get("/orders/{id}", orderController::getOrder);
        app.post("/orders/{id}/payment", orderController::processPayment);
        app.post("/orders/{id}/shipment", orderController::shipOrder);
        app.post("/orders/{id}/delivery", orderController::deliverOrder);
        app.post("/orders/{id}/cancellation", orderController::cancelOrder);

        app.get("/products/{sku}", productController::getProduct);

        app.get("/customers/{cpf}", customerController::getCustomer);
        app.get("/customers/{cpf}/notifications", customerController::getNotifications);
        app.post("/notifications/{id}/read", customerController::markNotificationAsRead);

        app.start(port);

        System.out.println("Server started on port " + port);
    }
}
