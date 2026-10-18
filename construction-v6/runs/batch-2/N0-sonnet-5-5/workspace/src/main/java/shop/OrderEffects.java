package shop;

import java.util.List;

/**
 * Consequências das mudanças de situação do pedido. Novas consequências (SMS, cashback...) são novas
 * implementações adicionadas à lista em {@link OrderService}.
 */
interface OrderEffect {
    /** Pagamento confirmado. Pode recusar (ApiException) desde que ainda não tenha alterado nada. */
    default void onPaid(Tx tx, Order order) {}

    /** Pedido já pago foi cancelado: desfaz o que {@link #onPaid} fez. */
    default void onPaidOrderCancelled(Tx tx, Order order) {}

    /** Qualquer cancelamento. */
    default void onCancelled(Tx tx, Order order) {}

    default void onShipped(Tx tx, Order order) {}
}

class StockEffect implements OrderEffect {
    @Override
    public void onPaid(Tx tx, Order order) {
        List<String> skus = order.items().stream().map(OrderItem::sku).toList();
        List<Product> locked = tx.lockProducts(skus);
        for (OrderItem item : order.items()) {
            Product p = locked.stream().filter(x -> x.sku().equals(item.sku())).findFirst().orElse(null);
            if (p == null || p.stock() < item.quantity()) throw ApiException.conflict("INSUFFICIENT_STOCK");
        }
        for (OrderItem item : order.items()) tx.addStock(item.sku(), -item.quantity());
    }

    @Override
    public void onPaidOrderCancelled(Tx tx, Order order) {
        tx.lockProducts(order.items().stream().map(OrderItem::sku).toList());
        for (OrderItem item : order.items()) tx.addStock(item.sku(), item.quantity());
    }
}

class ClubCreditEffect implements OrderEffect {
    @Override
    public void onPaid(Tx tx, Order order) {
        tx.addCredit(order.cpf(), order.summary().nextPurchaseCredit());
    }

    @Override
    public void onPaidOrderCancelled(Tx tx, Order order) {
        tx.addCredit(order.cpf(), order.summary().nextPurchaseCredit().negate());
    }
}

class NotificationEffect implements OrderEffect {
    @Override
    public void onPaid(Tx tx, Order order) {
        tx.addNotification(order.cpf(), NotificationType.PAYMENT_APPROVED, order.id());
    }

    @Override
    public void onCancelled(Tx tx, Order order) {
        tx.addNotification(order.cpf(), NotificationType.ORDER_CANCELLED, order.id());
    }

    @Override
    public void onShipped(Tx tx, Order order) {
        tx.addNotification(order.cpf(), NotificationType.ORDER_SHIPPED, order.id());
    }
}
