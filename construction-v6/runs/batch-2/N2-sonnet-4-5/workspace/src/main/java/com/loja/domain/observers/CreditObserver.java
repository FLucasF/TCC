package com.loja.domain.observers;

import com.loja.domain.entities.Customer;
import com.loja.domain.entities.Money;
import com.loja.domain.entities.Order;
import com.loja.domain.events.PaymentConfirmedEvent;
import com.loja.repositories.CustomerRepository;

public class CreditObserver implements PaymentConfirmedObserver {
    private final CustomerRepository customerRepository;

    public CreditObserver(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        Order order = event.getOrder();
        Money creditAmount = order.getSummary().getNextPurchaseCredit();

        if (creditAmount.isPositive()) {
            Customer customer = customerRepository.findByCpf(order.getCustomerCpf());
            customer.addCredit(creditAmount);
            customerRepository.update(customer);
        }
    }
}
