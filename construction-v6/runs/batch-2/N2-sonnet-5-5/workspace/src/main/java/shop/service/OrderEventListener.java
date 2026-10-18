package shop.service;

import shop.domain.Order;
import shop.persistence.Tx;

/**
 * Reage ao que acontece com um pedido. Um novo efeito (SMS, cashback...) é um novo listener
 * registrado em {@link Application}; {@link OrderService} não muda.
 * Os métodos rodam dentro da transação da operação.
 */
public interface OrderEventListener {
    /** Pode lançar {@link StockShortageException}; quem pode recusar deve ser o primeiro da lista e não alterar nada ao recusar. */
    default void onPaid(Tx tx, Order order) {
    }

    default void onShipped(Tx tx, Order order) {
    }

    default void onCancelled(Tx tx, Order order, boolean wasPaid) {
    }
}
