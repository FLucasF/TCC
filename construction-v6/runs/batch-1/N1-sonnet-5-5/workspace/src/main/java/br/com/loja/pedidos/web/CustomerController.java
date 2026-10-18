package br.com.loja.pedidos.web;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.loja.pedidos.common.DomainException;
import br.com.loja.pedidos.customer.Customer;
import br.com.loja.pedidos.customer.CustomerRepository;
import br.com.loja.pedidos.notification.Notification;
import br.com.loja.pedidos.notification.NotificationRepository;

@RestController
class CustomerController {

    private final CustomerRepository customers;
    private final NotificationRepository notifications;

    CustomerController(CustomerRepository customers, NotificationRepository notifications) {
        this.customers = customers;
        this.notifications = notifications;
    }

    @GetMapping("/customers/{cpf}")
    Customer get(@PathVariable String cpf) {
        return customers.find(cpf).orElseThrow(CustomerController::customerNotFound);
    }

    @GetMapping("/customers/{cpf}/notifications")
    List<Notification> notifications(@PathVariable String cpf) {
        customers.find(cpf).orElseThrow(CustomerController::customerNotFound);
        return notifications.listByCustomer(cpf);
    }

    @PostMapping("/notifications/{id}/read")
    @Transactional
    Notification read(@PathVariable String id) {
        notifications.find(id).orElseThrow(() -> DomainException.notFound("NOTIFICATION_NOT_FOUND"));
        notifications.markRead(id);
        return notifications.find(id).orElseThrow();
    }

    private static DomainException customerNotFound() {
        return DomainException.notFound("CUSTOMER_NOT_FOUND");
    }
}
