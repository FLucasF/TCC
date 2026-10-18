package shop.service;

import shop.domain.Order;
import shop.persistence.Tx;

public class StockListener implements OrderEventListener {
    @Override
    public void onPaid(Tx tx, Order order) {
        if (!tx.products().tryDebit(order.items())) {
            throw new StockShortageException();
        }
    }

    @Override
    public void onCancelled(Tx tx, Order order, boolean wasPaid) {
        if (wasPaid) {
            tx.products().restore(order.items());
        }
    }
}
