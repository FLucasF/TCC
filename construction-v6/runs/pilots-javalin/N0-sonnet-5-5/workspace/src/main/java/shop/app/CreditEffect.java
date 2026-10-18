package shop.app;

import java.sql.SQLException;
import shop.db.Dao;
import shop.domain.Order;

/** Club credit goes into the customer's balance on payment and comes out on cancellation. */
public final class CreditEffect implements PaidOrderEffect {
    @Override
    public void onPaid(Dao dao, Order order) throws SQLException {
        dao.addCredit(order.cpf(), order.summary().nextPurchaseCredit());
    }

    @Override
    public void onPaidOrderCancelled(Dao dao, Order order) throws SQLException {
        dao.addCredit(order.cpf(), order.summary().nextPurchaseCredit().negate());
    }
}
