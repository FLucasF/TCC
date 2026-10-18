package shop.app;

import java.sql.SQLException;
import shop.db.Dao;
import shop.domain.Order;

/**
 * Something that happens when an order's payment is confirmed (and is undone if the paid order is cancelled).
 * Future effects (SMS, cashback points) are new implementations added to the list in {@link OrderService}.
 */
public interface PaidOrderEffect {
    void onPaid(Dao dao, Order order) throws SQLException;

    void onPaidOrderCancelled(Dao dao, Order order) throws SQLException;
}
