package com.loja.checkout.domain.strategy;

public class AjustePagamentoCalculadorFactory {
    public static AjustePagamentoCalculador criar(String formaPagamento) {
        if (formaPagamento == null) {
            return null;
        }

        return switch (formaPagamento) {
            case "PIX" -> new AjustePagamentoPix();
            case "BOLETO" -> new AjustePagamentoBoleto();
            case "CARTAO" -> new AjustePagamentoCartao();
            default -> null;
        };
    }
}
