package com.loja.checkout.payment;

import java.math.BigDecimal;

public interface PaymentStrategy {

    String getCode();

    boolean isInstallmentValid(int installments);

    boolean isAvailable(BigDecimal orderTotal);

    PaymentResult calculate(BigDecimal orderTotal, int installments);
}
