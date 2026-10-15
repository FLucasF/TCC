package com.loja.checkout.pagamento;

import com.loja.checkout.model.FormaPagamento;

public class FabricaPagamento {
    public static CalculadoraPagamento criar(FormaPagamento forma) {
        if (forma == null) {
            return null;
        }

        return switch (forma) {
            case PIX -> new PagamentoPix();
            case CARTAO -> new PagamentoCartao();
            case BOLETO -> new PagamentoBoleto();
        };
    }
}
