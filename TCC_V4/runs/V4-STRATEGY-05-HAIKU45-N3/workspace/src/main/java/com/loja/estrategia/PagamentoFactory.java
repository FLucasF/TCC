package com.loja.estrategia;

import com.loja.enums.FormaPagamento;

public class PagamentoFactory {
    public static EstrategiaAjustePagamento criar(FormaPagamento tipo) {
        return switch (tipo) {
            case PIX -> new PagamentoPix();
            case CARTAO -> new PagamentoCartao();
            case BOLETO -> new PagamentoBoleto();
        };
    }
}
