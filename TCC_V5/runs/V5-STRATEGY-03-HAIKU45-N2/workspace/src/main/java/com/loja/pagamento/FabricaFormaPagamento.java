package com.loja.pagamento;

public class FabricaFormaPagamento {
    public static FormaPagamento criar(String forma) {
        if (forma == null) {
            return null;
        }

        return switch (forma) {
            case "PIX" -> new FormaPagamentoPix();
            case "BOLETO" -> new FormaPagamentoBoleto();
            case "CARTAO" -> new FormaPagamentoCartao();
            default -> null;
        };
    }
}
