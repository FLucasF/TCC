package com.loja.strategy;

import com.loja.model.PaymentMethod;

public class PaymentFactory {
    public static PaymentStrategy create(PaymentMethod method) {
        return switch (method) {
            case PIX -> new PixPayment();
            case CREDIT_CARD -> new CreditCardPayment();
            case BANK_SLIP -> new BankSlipPayment();
        };
    }
}
