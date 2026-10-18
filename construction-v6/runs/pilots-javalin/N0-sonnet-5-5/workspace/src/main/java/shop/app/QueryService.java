package shop.app;

import java.util.List;
import shop.db.Database;
import shop.domain.ApiException;
import shop.domain.Customer;
import shop.domain.Notification;
import shop.domain.Product;

public final class QueryService {
    private final Database db;

    public QueryService(Database db) {
        this.db = db;
    }

    public Product product(String sku) {
        return db.tx(d -> d.product(sku)).orElseThrow(() -> ApiException.notFound("PRODUCT_NOT_FOUND"));
    }

    public Customer customer(String cpf) {
        return db.tx(d -> d.customer(cpf)).orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND"));
    }

    public List<Notification> notifications(String cpf) {
        return db.tx(d -> {
            d.customer(cpf).orElseThrow(() -> ApiException.notFound("CUSTOMER_NOT_FOUND"));
            return d.notifications(cpf);
        });
    }

    public Notification markRead(String id) {
        return db.tx(d -> d.markRead(id)).orElseThrow(() -> ApiException.notFound("NOTIFICATION_NOT_FOUND"));
    }
}
