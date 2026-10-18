package shop;

import shop.catalog.Product;
import shop.customer.Customer;
import shop.notification.Notification;
import shop.notification.NotificationStatus;
import shop.persistence.Store;

import java.util.List;

/** Simple lookups: products, customers and their notifications. */
public final class QueryService {
    private final Store store;

    public QueryService(Store store) {
        this.store = store;
    }

    public Product product(String sku) {
        return store.inTransaction(tx -> tx.products().find(sku)
                .orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND")));
    }

    public Customer customer(String cpf) {
        return store.inTransaction(tx -> tx.customers().find(cpf)
                .orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND")));
    }

    public List<Notification> notifications(String cpf) {
        return store.inTransaction(tx -> {
            tx.customers().find(cpf).orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND"));
            return tx.notifications().findByCustomer(cpf);
        });
    }

    public Notification markRead(String id) {
        return store.inTransaction(tx -> {
            Notification notification = tx.notifications().find(id)
                    .orElseThrow(() -> ApiException.notFound("NOTIFICATION_NOT_FOUND"));
            tx.notifications().markRead(id);
            return new Notification(notification.id(), notification.type(), notification.orderId(),
                    NotificationStatus.READ, notification.customerCpf());
        });
    }
}
