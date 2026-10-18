package shop;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/** Armazenamento em memória, usado nos testes (sem banco). Uma transação por vez. */
class MemoryStore implements Store {
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, Product> products = new HashMap<>();
    private final Map<String, Order> orders = new HashMap<>();
    private final Map<String, Customer> customers = new HashMap<>();
    private final Map<String, PaymentRecord> payments = new HashMap<>();
    private final List<Notification> notifications = new ArrayList<>();

    MemoryStore() {
        for (Product p : Catalog.initial()) products.put(p.sku(), p);
    }

    @Override
    public <T> T transaction(Function<Tx, T> work) {
        lock.lock();
        try {
            return work.apply(new MemoryTx());
        } finally {
            lock.unlock();
        }
    }

    private final class MemoryTx implements Tx {
        public Optional<Product> findProduct(String sku) {
            return Optional.ofNullable(products.get(sku));
        }

        public List<Product> lockProducts(Collection<String> skus) {
            List<Product> result = new ArrayList<>();
            for (String sku : new TreeSet<>(skus)) {
                Product p = products.get(sku);
                if (p != null) result.add(p);
            }
            return result;
        }

        public void addStock(String sku, int delta) {
            Product p = products.get(sku);
            if (p.stock() + delta < 0) throw new IllegalStateException("negative stock");
            products.put(sku, new Product(sku, p.name(), p.price(), p.weightKg(), p.stock() + delta, p.discontinued()));
        }

        public Optional<Order> findOrder(String id) {
            return Optional.ofNullable(orders.get(id));
        }

        public Optional<Order> lockOrder(String id) {
            return findOrder(id);
        }

        public void insertOrder(Order order) {
            orders.put(order.id(), order);
        }

        public void updateOrder(Order order) {
            orders.put(order.id(), order);
        }

        public boolean hasFulfilledOrder(String cpf) {
            return orders.values().stream().anyMatch(o -> o.cpf().equals(cpf)
                    && (o.status() == OrderStatus.PAID || o.status() == OrderStatus.SHIPPED
                    || o.status() == OrderStatus.DELIVERED));
        }

        public void ensureCustomer(String cpf, String email) {
            customers.putIfAbsent(cpf, new Customer(cpf, Money.ZERO));
        }

        public Optional<Customer> findCustomer(String cpf) {
            return Optional.ofNullable(customers.get(cpf));
        }

        public void addCredit(String cpf, BigDecimal delta) {
            Customer c = customers.get(cpf);
            customers.put(cpf, new Customer(cpf, Money.round(c.creditBalance().add(delta))));
        }

        public Optional<PaymentRecord> findPayment(String orderId, String transactionId) {
            return Optional.ofNullable(payments.get(orderId + "|" + transactionId));
        }

        public void savePayment(String orderId, String transactionId, PaymentRecord record) {
            payments.put(orderId + "|" + transactionId, record);
        }

        public Notification addNotification(String cpf, NotificationType type, String orderId) {
            Notification n = new Notification(notifications.size() + 1L, cpf, type, orderId, NotificationStatus.NEW);
            notifications.add(n);
            return n;
        }

        public List<Notification> notifications(String cpf) {
            return notifications.stream().filter(n -> n.cpf().equals(cpf)).toList();
        }

        public Optional<Notification> markRead(long id) {
            if (id < 1 || id > notifications.size()) return Optional.empty();
            Notification n = notifications.get((int) id - 1);
            Notification read = new Notification(n.id(), n.cpf(), n.type(), n.orderId(), NotificationStatus.READ);
            notifications.set((int) id - 1, read);
            return Optional.of(read);
        }
    }
}
