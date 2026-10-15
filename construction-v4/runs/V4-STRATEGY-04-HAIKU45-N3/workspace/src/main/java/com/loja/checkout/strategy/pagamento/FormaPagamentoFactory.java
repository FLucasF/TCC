package com.loja.checkout.strategy.pagamento;

import com.loja.checkout.model.FormaPagamento;
import com.loja.checkout.exception.CheckoutException;

public class FormaPagamentoFactory {
    public static FormaPagamentoStrategy criar(FormaPagamento forma) {
        if (forma == null) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }

        return switch (forma) {
            case PIX -> new PixStrategy();
            case CARTAO -> new CartaoStrategy();
            case BOLETO -> new BoletoStrategy();
        };
    }
}
