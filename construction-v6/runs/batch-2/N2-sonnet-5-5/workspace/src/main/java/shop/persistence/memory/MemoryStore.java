package shop.persistence.memory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import shop.domain.CartItem;
import shop.domain.Customer;
import shop.domain.Money;
import shop.domain.Notification;
import shop.domain.NotificationStatus;
import shop.domain.NotificationType;
import shop.domain.Order;
import shop.domain.OrderStatus;
import shop.domain.Product;
import shop.persistence.CustomerRepository;
import shop.persistence.NotificationRepository;
import shop.persistence.OrderRepository;
import shop.persistence.PaymentRepository;
import shop.persistence.ProductRepository;
import shop.persistence.Store;
import shop.persistence.Tx;

/** Implementação em memória (usada nos testes). Não desfaz alterações em caso de erro. */
public class MemoryStore implements Store, Tx {
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, Product> products = new LinkedHashMap<>();
    private final Map<String, Order> orders = new LinkedHashMap<>();
    private final Map<String, Customer> customers = new HashMap<>();
    private final List<Notification> notifications = new ArrayList<>();
    private final Map<String, PaymentRepository.PaymentRecord> payments = new HashMap<>();

    @Override
    public <T> T inTransaction(Function<Tx, T> work) {
        lock.lock();
        try {
            return work.apply(this);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ProductRepository products() {
        return new ProductRepository() {
            public Optional<Product> find(String sku) {
                return Optional.ofNullable(products.get(sku));
            }

            public void insertIfAbsent(Product p) {
                products.putIfAbsent(p.sku(), p);
            }

            public boolean tryDebit(List<CartItem> items) {
                for (CartItem i : items) {
                    Product p = products.get(i.sku());
                    if (p == null || p.stock() < i.quantity()) {
                        return false;
                    }
                }
                items.forEach(i -> adjust(i.sku(), -i.quantity()));
                return true;
            }

            public void restore(List<CartItem> items) {
                items.forEach(i -> adjust(i.sku(), i.quantity()));
            }

            private void adjust(String sku, int delta) {
                Product p = products.get(sku);
                products.put(sku, new Product(p.sku(), p.name(), p.price(), p.weightKg(), p.stock() + delta,
                        p.discontinued()));
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

            public Optional<Order> findForUpdate(String id) {
                return find(id);
            }

            public void update(Order order) {
                orders.put(order.id(), order);
            }

            public boolean hasPurchased(String cpf) {
                return orders.values().stream().anyMatch(o -> o.cpf().equals(cpf)
                        && (o.status() == OrderStatus.PAID || o.status() == OrderStatus.SHIPPED
                        || o.status() == OrderStatus.DELIVERED));
            }
        };
    }

    @Override
    public CustomerRepository customers() {
        return new CustomerRepository() {
            public Optional<Customer> find(String cpf) {
                return Optional.ofNullable(customers.get(cpf));
            }

            public void ensure(String cpf) {
                customers.putIfAbsent(cpf, new Customer(cpf, Money.ZERO));
            }

            public void addCredit(String cpf, BigDecimal amount) {
                Customer c = customers.get(cpf);
                customers.put(cpf, new Customer(cpf, c.creditBalance().add(amount)));
            }
        };
    }

    @Override
    public NotificationRepository notifications() {
        return new NotificationRepository() {
            public void add(String cpf, NotificationType type, String orderId) {
                notifications.add(new Notification(UUID.randomUUID().toString(), type, orderId,
                        NotificationStatus.NEW, cpf));
            }

            public List<Notification> findByCpf(String cpf) {
                return notifications.stream().filter(n -> n.cpf().equals(cpf)).toList();
            }

            public Optional<Notification> find(String id) {
                return notifications.stream().filter(n -> n.id().equals(id)).findFirst();
            }

            public void markRead(String id) {
                for (int i = 0; i < notifications.size(); i++) {
                    Notification n = notifications.get(i);
                    if (n.id().equals(id)) {
                        notifications.set(i, new Notification(n.id(), n.type(), n.orderId(),
                                NotificationStatus.READ, n.cpf()));
                    }
                }
            }
        };
    }

    @Override
    public PaymentRepository payments() {
        return new PaymentRepository() {
            public Optional<PaymentRepository.PaymentRecord> find(String orderId, String transactionId) {
                return Optional.ofNullable(payments.get(orderId + "|" + transactionId));
            }

            public void record(String orderId, String transactionId, String errorCode) {
                payments.put(orderId + "|" + transactionId, new PaymentRepository.PaymentRecord(errorCode));
            }
        };
    }
}
