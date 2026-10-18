package shop.order;

import shop.notification.NotificationType;
import shop.notification.Notifier;
import shop.persistence.Tx;

public final class PaymentApprovedNoticeReaction implements PaymentConfirmationReaction {
    private final Notifier notifier;

    public PaymentApprovedNoticeReaction(Notifier notifier) {
        this.notifier = notifier;
    }

    @Override
    public void onPaid(Tx tx, Order order) {
        notifier.notify(tx, order, NotificationType.PAYMENT_APPROVED);
    }
}
