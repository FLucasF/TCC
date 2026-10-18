package com.clothing;

import com.clothing.model.ErrorResponse;
import com.clothing.service.CustomerRepository;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerRoutes {
    private static final Logger logger = LoggerFactory.getLogger(CustomerRoutes.class);
    private final Javalin app;
    private final CustomerRepository customerRepo = new CustomerRepository();

    public CustomerRoutes(Javalin app) {
        this.app = app;
    }

    public void register() {
        app.get("/customers/{cpf}", ctx -> {
            try {
                String cpf = ctx.pathParam("cpf");
                var customer = customerRepo.getCustomer(cpf);
                if (customer == null) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("CUSTOMER_NOT_FOUND"));
                    return;
                }
                ctx.status(200);
                ctx.json(customer);
            } catch (Exception e) {
                logger.error("Error getting customer", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR"));
            }
        });

        app.get("/customers/{cpf}/notifications", ctx -> {
            try {
                String cpf = ctx.pathParam("cpf");
                var customer = customerRepo.getCustomer(cpf);
                if (customer == null) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("CUSTOMER_NOT_FOUND"));
                    return;
                }
                var notifications = customerRepo.getNotifications(cpf);
                ctx.status(200);
                ctx.json(notifications);
            } catch (Exception e) {
                logger.error("Error getting notifications", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR"));
            }
        });

        app.post("/notifications/{id}/read", ctx -> {
            try {
                long notificationId = Long.parseLong(ctx.pathParam("id"));
                var notification = customerRepo.getNotification(notificationId);
                if (notification == null) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("NOTIFICATION_NOT_FOUND"));
                    return;
                }
                customerRepo.markNotificationAsRead(notificationId);
                var updatedNotification = customerRepo.getNotification(notificationId);
                ctx.status(200);
                ctx.json(updatedNotification);
            } catch (Exception e) {
                logger.error("Error marking notification as read", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR"));
            }
        });
    }
}
