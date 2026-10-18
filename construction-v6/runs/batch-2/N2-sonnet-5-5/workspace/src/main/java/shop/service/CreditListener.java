package shop.service;

import shop.domain.Order;
import shop.persistence.Tx;

public class CreditListener implements OrderEventListener {
    @Override
    public void onPaid(Tx tx, Order order) {
        tx.customers().addCredit(order.cpf(), order.summary().nextPurchaseCredit());
    }

    @Override
    public void onCancelled(Tx tx, Order order, boolean wasPaid) {
        if (wasPaid) {
            tx.customers().addCredit(order.cpf(), order.summary().nextPurchaseCredit().negate());
        }
    }
}
