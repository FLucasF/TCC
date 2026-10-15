package com.loja.checkout.estrategia;

import com.loja.checkout.domain.FormaPagamento;

public class FabricaFormaPagamento {
    public static EstrategiaFormaPagamento criar(FormaPagamento forma) {
        return switch (forma) {
            case PIX -> new PagamentoPix();
            case CARTAO -> new PagamentoCartao();
            case BOLETO -> new PagamentoBoleto();
        };
    }
}
