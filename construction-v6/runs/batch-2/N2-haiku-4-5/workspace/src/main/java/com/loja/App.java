package com.loja;

import io.javalin.Javalin;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.loja.api.*;
import com.loja.repository.*;
import com.loja.service.*;

public class App {
    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        var db = new Database();
        db.initialize();

        var productRepo = new ProductRepository(db);
        var orderRepo = new OrderRepository(db);
        var customerRepo = new CustomerRepository(db);
        var notificationRepo = new NotificationRepository(db);

        var orderService = new OrderService(productRepo, orderRepo, customerRepo, notificationRepo);
        var checkoutService = new CheckoutService(productRepo, orderService);

        ObjectMapper jackson = new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        var app = Javalin.create(config -> {
        }).start(port);

        new CheckoutEndpoint(checkoutService).register(app);
        new OrderEndpoint(orderService).register(app);
        new ProductEndpoint(productRepo).register(app);
        new CustomerEndpoint(customerRepo).register(app);
        new NotificationEndpoint(notificationRepo).register(app);

        System.out.println("Order service running on port " + port);
    }
}
