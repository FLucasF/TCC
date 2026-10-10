package com.loja.service.estrategia;

import com.loja.domain.FormaPagamento;

public class FabricaPagamento {
    public static CalculoPagamento criar(FormaPagamento forma, Integer parcelas) {
        return switch (forma) {
            case PIX -> new PagamentoPix();
            case BOLETO -> new PagamentoBoleto();
            case CARTAO -> new PagamentoCartao(parcelas);
        };
    }
}
