package br.com.loja.customer;

import org.springframework.stereotype.Component;

import br.com.loja.order.Order;
import br.com.loja.order.OrderEventListener;

/** Entra com o crédito do clube quando o pagamento é confirmado e tira de volta se o pedido pago é cancelado. */
@Component
class LoyaltyCreditListener implements OrderEventListener {

    private final CustomerRepository customers;

    LoyaltyCreditListener(CustomerRepository customers) {
        this.customers = customers;
    }

    @Override
    public void onPaid(Order order) {
        customers.addCredit(order.customerCpf(), order.summary().nextPurchaseCredit());
    }

    @Override
    public void onCancelled(Order order, boolean wasPaid) {
        if (wasPaid) {
            customers.addCredit(order.customerCpf(), order.summary().nextPurchaseCredit().negate());
        }
    }
}
