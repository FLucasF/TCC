package shop;

import shop.catalog.InitialCatalog;
import shop.catalog.Product;
import shop.catalog.ProductRepository;
import shop.customer.Customer;
import shop.customer.CustomerRepository;
import shop.notification.Notification;
import shop.notification.NotificationRepository;
import shop.notification.NotificationStatus;
import shop.order.Order;
import shop.order.OrderRepository;
import shop.order.OrderStatus;
import shop.order.PaymentNotice;
import shop.order.PaymentNoticeRepository;
import shop.persistence.Store;
import shop.persistence.Tx;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/** Test double: one global lock stands for the database's transactions. */
final class InMemoryStore implements Store, Tx {
    final Map<String, Product> products = new LinkedHashMap<>();
    final Map<String, Customer> customers = new LinkedHashMap<>();
    final Map<String, Order> orders = new LinkedHashMap<>();
    final List<Notification> notifications = new ArrayList<>();
    final Map<String, PaymentNotice> paymentNotices = new LinkedHashMap<>();

    InMemoryStore() {
        InitialCatalog.PRODUCTS.forEach(p -> products.put(p.sku(), p));
    }

    @Override
    public synchronized <T> T inTransaction(Function<Tx, T> work) {
        return work.apply(this);
    }

    @Override
    public ProductRepository products() {
        return new ProductRepository() {
            public Optional<Product> find(String sku) {
                return Optional.ofNullable(products.get(sku));
            }

            public Optional<Product> lock(String sku) {
                return find(sku);
            }

            public void adjustStock(String sku, int delta) {
                Product p = products.get(sku);
                if (p.stock() + delta < 0) {
                    throw new IllegalStateException("negative stock");
                }
                products.put(sku, new Product(sku, p.name(), p.price(), p.weightKg(), p.stock() + delta, p.discontinued()));
            }

            public void insertIfAbsent(Product p) {
                products.putIfAbsent(p.sku(), p);
            }
        };
    }

    @Override
    public OrderRepository orders() {
        return new OrderRepository() {
            public void insert(Order order) {
                orders.put(order.id(), order);
            }

            public Optional<Order> find(String id) {
                return Optional.ofNullable(orders.get(id));
            }

            public Optional<Order> lock(String id) {
                return find(id);
            }

            public void update(Order order) {
                orders.put(order.id(), order);
            }

            public boolean hasConfirmedPurchase(String cpf) {
                Set<OrderStatus> confirmed = Set.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED);
                return orders.values().stream().anyMatch(o -> o.customerCpf().equals(cpf) && confirmed.contains(o.status()));
            }
        };
    }

    @Override
    public CustomerRepository customers() {
        return new CustomerRepository() {
            public void createIfAbsent(String cpf) {
                customers.putIfAbsent(cpf, new Customer(cpf, BigDecimal.ZERO));
            }

            public Optional<Customer> find(String cpf) {
                return Optional.ofNullable(customers.get(cpf));
            }

            public void addCredit(String cpf, BigDecimal amount) {
                customers.put(cpf, new Customer(cpf, customers.get(cpf).creditBalance().add(amount)));
            }
        };
    }

    @Override
    public NotificationRepository notifications() {
        return new NotificationRepository() {
            public void insert(Notification n) {
                notifications.add(n);
            }

            public List<Notification> findByCustomer(String cpf) {
                return notifications.stream().filter(n -> n.customerCpf().equals(cpf)).toList();
            }

            public Optional<Notification> find(String id) {
                return notifications.stream().filter(n -> n.id().equals(id)).findFirst();
            }

            public void markRead(String id) {
                notifications.replaceAll(n -> n.id().equals(id)
                        ? new Notification(n.id(), n.type(), n.orderId(), NotificationStatus.READ, n.customerCpf()) : n);
            }
        };
    }

    @Override
    public PaymentNoticeRepository paymentNotices() {
        return new PaymentNoticeRepository() {
            public Optional<PaymentNotice> find(String orderId, String transactionId) {
                return Optional.ofNullable(paymentNotices.get(orderId + "|" + transactionId));
            }

            public void insert(PaymentNotice n) {
                paymentNotices.put(n.orderId() + "|" + n.transactionId(), n);
            }
        };
    }
}
