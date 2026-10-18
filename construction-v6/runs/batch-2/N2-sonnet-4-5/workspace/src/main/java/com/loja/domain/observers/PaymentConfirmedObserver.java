package com.loja.domain.observers;

import com.loja.domain.events.PaymentConfirmedEvent;

public interface PaymentConfirmedObserver {
    void onPaymentConfirmed(PaymentConfirmedEvent event) throws Exception;
}
