package shop.persistence.postgres;

import java.sql.Connection;
import shop.persistence.CustomerRepository;
import shop.persistence.NotificationRepository;
import shop.persistence.OrderRepository;
import shop.persistence.PaymentRepository;
import shop.persistence.ProductRepository;
import shop.persistence.Tx;

class PostgresTx implements Tx {
    private final Connection connection;

    PostgresTx(Connection connection) {
        this.connection = connection;
    }

    @Override
    public ProductRepository products() {
        return new PgProducts(connection);
    }

    @Override
    public OrderRepository orders() {
        return new PgOrders(connection);
    }

    @Override
    public CustomerRepository customers() {
        return new PgCustomers(connection);
    }

    @Override
    public NotificationRepository notifications() {
        return new PgNotifications(connection);
    }

    @Override
    public PaymentRepository payments() {
        return new PgPayments(connection);
    }
}
