package com.loja.orders.catalog;

import org.springframework.stereotype.Component;

import com.loja.orders.order.Order;
import com.loja.orders.order.OrderLifecycleListener;
import com.loja.orders.order.OrderStatus;

/** Quando um pedido pago é cancelado, o estoque volta. */
@Component
public class StockReturnListener implements OrderLifecycleListener {

    private final InventoryService inventory;

    public StockReturnListener(InventoryService inventory) {
        this.inventory = inventory;
    }

    @Override
    public void cancelled(Order order, OrderStatus previousStatus) {
        if (previousStatus.paymentSettled()) {
            inventory.giveBack(order.items());
        }
    }
}
