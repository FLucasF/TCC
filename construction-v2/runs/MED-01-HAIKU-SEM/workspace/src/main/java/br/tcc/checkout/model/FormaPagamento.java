package br.tcc.checkout.model;

public enum FormaPagamento {
    PIX,
    CARTAO,
    BOLETO;

    public static FormaPagamento fromString(String valor) {
        try {
            return FormaPagamento.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
