package com.loja.checkout.strategy;

import com.loja.checkout.enums.FormaPagamento;
import com.loja.checkout.strategy.payment.*;

public class PaymentAdjustmentStrategyFactory {
    public static PaymentAdjustmentStrategy create(FormaPagamento forma) {
        return switch (forma) {
            case PIX -> new PixPayment();
            case CARTAO -> new CartaoPayment();
            case BOLETO -> new BoletoPayment();
        };
    }
}
