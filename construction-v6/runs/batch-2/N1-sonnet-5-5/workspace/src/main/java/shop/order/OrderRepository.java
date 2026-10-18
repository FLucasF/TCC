package shop.order;

import java.util.Optional;

public interface OrderRepository {
    void insert(Order order);

    Optional<Order> find(String id);

    /** Reads the order locking it until the end of the transaction. */
    Optional<Order> lock(String id);

    void update(Order order);

    /** Whether the customer has an order in a status that confirms a purchase (paid, shipped or delivered). */
    boolean hasConfirmedPurchase(String cpf);
}
