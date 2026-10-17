package com.loja.orders.payment;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.loja.orders.common.ApiException;
import com.loja.orders.common.ErrorCode;
import com.loja.orders.common.Money;

/** Boleto: à vista, com a tarifa do banco, até R$ 1.000,00 de total do pedido. */
@Component
public class BankSlipPayment implements PaymentMethod {

    private static final BigDecimal FEE = Money.of("3.49");
    private static final BigDecimal MAX_ORDER_TOTAL = Money.of("1000.00");

    @Override
    public String code() {
        return "BANK_SLIP";
    }

    @Override
    public boolean acceptsInstallments(int installments) {
        return installments == 1;
    }

    @Override
    public PaymentTerms terms(PaymentContext context) {
        if (context.orderTotal().compareTo(MAX_ORDER_TOTAL) > 0) {
            throw ApiException.unprocessable(ErrorCode.PAYMENT_METHOD_UNAVAILABLE);
        }
        return PaymentTerms.upFront(context.orderTotal().add(FEE));
    }
}
