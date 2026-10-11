package com.loja.checkout.strategy;

import com.loja.checkout.enums.FormaPagamento;

public class PagamentoFactory {

    public static PagamentoStrategy criar(FormaPagamento formaPagamento) {
        return switch (formaPagamento) {
            case PIX -> new PagamentoPix();
            case CARTAO -> new PagamentoCartao();
            case BOLETO -> new PagamentoBoleto();
        };
    }
}
