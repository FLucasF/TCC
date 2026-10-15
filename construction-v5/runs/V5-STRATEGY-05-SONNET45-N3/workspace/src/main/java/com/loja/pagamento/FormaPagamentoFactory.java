package com.loja.pagamento;

public class FormaPagamentoFactory {

    public static FormaPagamento criar(String codigo) {
        return switch (codigo) {
            case "PIX" -> new Pix();
            case "BOLETO" -> new Boleto();
            case "CARTAO" -> new Cartao();
            default -> null;
        };
    }
}
