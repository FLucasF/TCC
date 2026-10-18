package shop.persistence;

import shop.catalog.ProductRepository;
import shop.customer.CustomerRepository;
import shop.notification.NotificationRepository;
import shop.order.OrderRepository;
import shop.order.PaymentNoticeRepository;

/** Repositories bound to one transaction. */
public interface Tx {
    ProductRepository products();

    OrderRepository orders();

    CustomerRepository customers();

    NotificationRepository notifications();

    PaymentNoticeRepository paymentNotices();
}
