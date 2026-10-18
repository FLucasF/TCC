package shop.service;

import java.util.List;
import shop.domain.Customer;
import shop.domain.DomainException;
import shop.domain.ErrorCode;
import shop.domain.Notification;
import shop.domain.Product;
import shop.persistence.Store;

public class QueryService {
    private final Store store;

    public QueryService(Store store) {
        this.store = store;
    }

    public Product product(String sku) {
        return store.inTransaction(tx -> tx.products().find(sku)
                .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND)));
    }

    public Customer customer(String cpf) {
        return store.inTransaction(tx -> tx.customers().find(cpf)
                .orElseThrow(() -> new DomainException(ErrorCode.CUSTOMER_NOT_FOUND)));
    }

    public List<Notification> notifications(String cpf) {
        return store.inTransaction(tx -> {
            tx.customers().find(cpf).orElseThrow(() -> new DomainException(ErrorCode.CUSTOMER_NOT_FOUND));
            return tx.notifications().findByCpf(cpf);
        });
    }

    public Notification markRead(String id) {
        return store.inTransaction(tx -> {
            tx.notifications().find(id)
                    .orElseThrow(() -> new DomainException(ErrorCode.NOTIFICATION_NOT_FOUND));
            tx.notifications().markRead(id);
            return tx.notifications().find(id).orElseThrow();
        });
    }
}
