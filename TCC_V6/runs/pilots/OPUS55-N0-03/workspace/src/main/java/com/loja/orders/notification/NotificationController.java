package com.loja.orders.notification;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loja.orders.common.ApiException;
import com.loja.orders.common.ErrorCode;
import com.loja.orders.customer.CustomerRepository;

@RestController
public class NotificationController {

    private final NotificationRepository notifications;
    private final CustomerRepository customers;

    public NotificationController(NotificationRepository notifications, CustomerRepository customers) {
        this.notifications = notifications;
        this.customers = customers;
    }

    @GetMapping("/customers/{cpf}/notifications")
    public List<Notification> list(@PathVariable String cpf) {
        if (!customers.exists(cpf)) {
            throw ApiException.notFound(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        return notifications.findByCpf(cpf);
    }

    @PostMapping("/notifications/{id}/read")
    public Notification markRead(@PathVariable String id) {
        return notifications.markRead(id).orElseThrow(() -> ApiException.notFound(ErrorCode.NOTIFICATION_NOT_FOUND));
    }
}
