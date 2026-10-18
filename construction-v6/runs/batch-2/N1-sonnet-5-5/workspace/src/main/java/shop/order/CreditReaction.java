package shop.order;

import shop.persistence.Tx;

public final class CreditReaction implements PaymentConfirmationReaction {
    @Override
    public void onPaid(Tx tx, Order order) {
        tx.customers().addCredit(order.customerCpf(), order.summary().nextPurchaseCredit());
    }

    @Override
    public void onPaidOrderCancelled(Tx tx, Order order) {
        tx.customers().addCredit(order.customerCpf(), order.summary().nextPurchaseCredit().negate());
    }
}
