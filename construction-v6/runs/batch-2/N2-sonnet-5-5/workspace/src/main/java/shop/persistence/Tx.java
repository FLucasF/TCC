package shop.persistence;

public interface Tx {
    ProductRepository products();

    OrderRepository orders();

    CustomerRepository customers();

    NotificationRepository notifications();

    PaymentRepository payments();
}
