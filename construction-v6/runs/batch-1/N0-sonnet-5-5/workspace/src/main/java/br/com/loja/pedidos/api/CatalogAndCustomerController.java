package br.com.loja.pedidos.api;

import br.com.loja.pedidos.catalog.Product;
import br.com.loja.pedidos.catalog.ProductRepository;
import br.com.loja.pedidos.common.ApiException;
import br.com.loja.pedidos.customer.Customer;
import br.com.loja.pedidos.customer.CustomerRepository;
import br.com.loja.pedidos.notification.Notification;
import br.com.loja.pedidos.notification.NotificationRepository;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CatalogAndCustomerController {

    private final ProductRepository products;
    private final CustomerRepository customers;
    private final NotificationRepository notifications;

    CatalogAndCustomerController(ProductRepository products, CustomerRepository customers,
                                 NotificationRepository notifications) {
        this.products = products;
        this.customers = customers;
        this.notifications = notifications;
    }

    @GetMapping("/products/{sku}")
    Product product(@PathVariable String sku) {
        return products.find(sku).orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND"));
    }

    @GetMapping("/customers/{cpf}")
    Customer customer(@PathVariable String cpf) {
        return customers.find(cpf).orElseThrow(CatalogAndCustomerController::customerNotFound);
    }

    @GetMapping("/customers/{cpf}/notifications")
    List<Notification> notifications(@PathVariable String cpf) {
        customers.find(cpf).orElseThrow(CatalogAndCustomerController::customerNotFound);
        return notifications.findByCpf(cpf);
    }

    @PostMapping("/notifications/{id}/read")
    @Transactional
    Notification read(@PathVariable String id) {
        notifications.find(id).orElseThrow(() -> ApiException.notFound("NOTIFICATION_NOT_FOUND"));
        notifications.markRead(id);
        return notifications.find(id).orElseThrow();
    }

    private static ApiException customerNotFound() {
        return ApiException.notFound("CUSTOMER_NOT_FOUND");
    }
}
