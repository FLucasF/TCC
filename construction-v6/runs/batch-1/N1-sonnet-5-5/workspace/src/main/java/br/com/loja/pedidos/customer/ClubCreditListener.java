package br.com.loja.pedidos.customer;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.order.Order;
import br.com.loja.pedidos.order.OrderEventListener;

@Component
class ClubCreditListener implements OrderEventListener {

    private final CustomerRepository customers;

    ClubCreditListener(CustomerRepository customers) {
        this.customers = customers;
    }

    @Override
    public void paymentConfirmed(Order order) {
        customers.addCredit(order.cpf(), order.summary().nextPurchaseCredit());
    }

    @Override
    public void cancelled(Order order, boolean wasPaid) {
        if (wasPaid) {
            customers.addCredit(order.cpf(), order.summary().nextPurchaseCredit().negate());
        }
    }
}
