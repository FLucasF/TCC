package shop.order;

import shop.persistence.Tx;

/** Something that must happen when a payment is confirmed (and be undone if the paid order is cancelled). */
public interface PaymentConfirmationReaction {
    void onPaid(Tx tx, Order order);

    default void onPaidOrderCancelled(Tx tx, Order order) {
    }
}
