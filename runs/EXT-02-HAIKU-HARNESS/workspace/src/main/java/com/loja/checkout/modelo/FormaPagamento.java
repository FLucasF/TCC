package com.loja.checkout.modelo;

public enum FormaPagamento {
    PIX("PIX"),
    CARTAO("CARTAO"),
    BOLETO("BOLETO");

    private final String codigo;

    FormaPagamento(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public boolean permiteParcelacao() {
        return this == CARTAO;
    }

    public boolean podeParcelarEm(int parcelas) {
        if (this == PIX || this == BOLETO) {
            return parcelas == 1;
        }
        return this == CARTAO && parcelas >= 1 && parcelas <= 12;
    }
}
