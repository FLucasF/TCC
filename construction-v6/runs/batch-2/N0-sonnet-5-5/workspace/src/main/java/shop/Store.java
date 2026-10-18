package shop;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Armazenamento: toda operação roda dentro de uma transação. */
interface Store {
    <T> T transaction(Function<Tx, T> work);
}

/**
 * Operações dentro de uma transação. Ordem de travas usada por todos os fluxos, para evitar deadlock:
 * pedido, depois produtos (por SKU), depois cliente.
 */
interface Tx {
    Optional<Product> findProduct(String sku);

    /** Trava os produtos para alteração, em ordem de SKU, e devolve o estado atual. */
    List<Product> lockProducts(Collection<String> skus);

    void addStock(String sku, int delta);

    Optional<Order> findOrder(String id);

    Optional<Order> lockOrder(String id);

    void insertOrder(Order order);

    void updateOrder(Order order);

    /** Existe pedido pago, enviado ou entregue para o CPF? */
    boolean hasFulfilledOrder(String cpf);

    void ensureCustomer(String cpf, String email);

    Optional<Customer> findCustomer(String cpf);

    void addCredit(String cpf, BigDecimal delta);

    Optional<PaymentRecord> findPayment(String orderId, String transactionId);

    void savePayment(String orderId, String transactionId, PaymentRecord record);

    Notification addNotification(String cpf, NotificationType type, String orderId);

    List<Notification> notifications(String cpf);

    Optional<Notification> markRead(long id);
}

final class Catalog {
    private Catalog() {}

    static List<Product> initial() {
        return List.of(
                new Product("CAM-001", "Camiseta básica", new BigDecimal("79.90"), new BigDecimal("0.30"), 50, false),
                new Product("TEN-001", "Tênis corrida", new BigDecimal("249.90"), new BigDecimal("1.20"), 10, false),
                new Product("FON-001", "Fone sem fio", new BigDecimal("199.90"), new BigDecimal("0.25"), 5, false),
                new Product("MEI-001", "Meia esportiva", new BigDecimal("19.90"), new BigDecimal("0.10"), 100, false),
                new Product("JAQ-001", "Jaqueta corta-vento", new BigDecimal("389.90"), new BigDecimal("0.80"), 3, false),
                new Product("MOC-001", "Mochila urbana", new BigDecimal("159.90"), new BigDecimal("0.90"), 1, false),
                new Product("BON-001", "Boné aba reta", new BigDecimal("59.90"), new BigDecimal("0.15"), 0, false),
                new Product("REL-001", "Relógio digital", new BigDecimal("129.90"), new BigDecimal("0.20"), 8, true));
    }
}
