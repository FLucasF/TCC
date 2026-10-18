package shop.persistence;

import java.util.Optional;
import shop.domain.Order;

public interface OrderRepository {
    void insert(Order order);

    Optional<Order> find(String id);

    /** Busca e trava o pedido até o fim da transação. */
    Optional<Order> findForUpdate(String id);

    void update(Order order);

    /** Se o CPF tem algum pedido pago, enviado ou entregue. */
    boolean hasPurchased(String cpf);
}
