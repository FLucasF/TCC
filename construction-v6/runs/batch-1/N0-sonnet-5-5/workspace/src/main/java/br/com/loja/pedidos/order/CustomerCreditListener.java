package br.com.loja.pedidos.order;

import br.com.loja.pedidos.customer.CustomerRepository;
import org.springframework.stereotype.Component;

@Component
class CustomerCreditListener implements OrderLifecycleListener {

    private final CustomerRepository customers;

    CustomerCreditListener(CustomerRepository customers) {
        this.customers = customers;
    }

    @Override
    public void onPaid(StoredOrder order) {
        customers.addCredit(order.cpf(), order.order().summary().nextPurchaseCredit());
    }

    @Override
    public void onCancelled(StoredOrder order, boolean wasPaid) {
        if (wasPaid) {
            customers.addCredit(order.cpf(), order.order().summary().nextPurchaseCredit().negate());
        }
    }
}
