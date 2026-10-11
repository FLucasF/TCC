package com.loja.checkout.strategy;

public class FormaPagamentoFactory {

    public static FormaPagamento criar(String forma) {
        return switch (forma) {
            case "PIX" -> new FormaPagamentoPix();
            case "BOLETO" -> new FormaPagamentoBoleto();
            case "CARTAO" -> new FormaPagamentoCartao();
            default -> null;
        };
    }

}
