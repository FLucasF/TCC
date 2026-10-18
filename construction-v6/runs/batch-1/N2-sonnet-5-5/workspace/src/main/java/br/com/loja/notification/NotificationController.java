package br.com.loja.notification;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.loja.common.ApiException;
import br.com.loja.common.ErrorCode;
import br.com.loja.customer.CustomerRepository;

@RestController
class NotificationController {

    private final NotificationRepository notifications;
    private final CustomerRepository customers;

    NotificationController(NotificationRepository notifications, CustomerRepository customers) {
        this.notifications = notifications;
        this.customers = customers;
    }

    @GetMapping("/customers/{cpf}/notifications")
    List<Notification> list(@PathVariable String cpf) {
        if (!customers.exists(cpf)) {
            throw new ApiException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        return notifications.findByCustomer(cpf);
    }

    @PostMapping("/notifications/{id}/read")
    Notification read(@PathVariable String id) {
        try {
            return notifications.markRead(Long.parseLong(id))
                    .orElseThrow(() -> new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND));
        } catch (NumberFormatException e) {
            throw new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
    }
}
