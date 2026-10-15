package com.loja.orders.customer;

import org.springframework.stereotype.Component;

import com.loja.orders.order.Order;
import com.loja.orders.order.OrderLifecycleListener;
import com.loja.orders.order.OrderStatus;

/** O crédito do clube entra no saldo quando o pagamento é confirmado e sai se o pedido pago é cancelado. */
@Component
public class ClubCreditListener implements OrderLifecycleListener {

    private final CustomerRepository customers;

    public ClubCreditListener(CustomerRepository customers) {
        this.customers = customers;
    }

    @Override
    public void paymentApproved(Order order) {
        customers.addCredit(order.cpf(), order.summary().nextPurchaseCredit());
    }

    @Override
    public void cancelled(Order order, OrderStatus previousStatus) {
        if (previousStatus.paymentSettled()) {
            customers.addCredit(order.cpf(), order.summary().nextPurchaseCredit().negate());
        }
    }
}
