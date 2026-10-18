package shop.app;

import java.sql.SQLException;
import shop.db.Dao;
import shop.domain.NotificationType;
import shop.domain.Order;

public final class PaymentApprovedNoticeEffect implements PaidOrderEffect {
    private final Notifier notifier;

    public PaymentApprovedNoticeEffect(Notifier notifier) {
        this.notifier = notifier;
    }

    @Override
    public void onPaid(Dao dao, Order order) throws SQLException {
        notifier.notify(dao, order, NotificationType.PAYMENT_APPROVED);
    }

    @Override
    public void onPaidOrderCancelled(Dao dao, Order order) {
        // the cancellation notice is sent for every cancellation by OrderService
    }
}
