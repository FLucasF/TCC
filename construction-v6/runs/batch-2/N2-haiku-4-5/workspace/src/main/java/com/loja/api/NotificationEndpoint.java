package com.loja.api;

import io.javalin.Javalin;
import com.loja.model.*;
import com.loja.repository.NotificationRepository;
import com.loja.repository.CustomerRepository;
import java.util.List;

public class NotificationEndpoint {
    private final NotificationRepository notificationRepo;

    public NotificationEndpoint(NotificationRepository notificationRepo) {
        this.notificationRepo = notificationRepo;
    }

    public void register(Javalin app) {
        app.get("/customers/{cpf}/notifications", ctx -> {
            List<Notification> notifications = notificationRepo.findByCpf(ctx.pathParam("cpf"));
            if (notifications.isEmpty()) {
                ctx.status(404).json(new ErrorResponse("CUSTOMER_NOT_FOUND"));
                return;
            }
            ctx.status(200).json(notifications);
        });

        app.post("/notifications/{id}/read", ctx -> {
            Notification notification = notificationRepo.findById(ctx.pathParam("id"));
            if (notification == null) {
                ctx.status(404).json(new ErrorResponse("NOTIFICATION_NOT_FOUND"));
                return;
            }
            notificationRepo.markAsRead(notification.id);
            notification.status = NotificationStatus.READ;
            ctx.status(200).json(notification);
        });
    }
}
