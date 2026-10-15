package br.tcc.checkout.model;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2),
    RETIRADA_LOJA(new BigDecimal("0.00"), new BigDecimal("0.00"), 1),
    MOTOBOY(new BigDecimal("18.00"), new BigDecimal("0.00"), 0);

    private final BigDecimal valorFixo;
    private final BigDecimal valorPorKg;
    private final int prazo;

    ModalidadeEntrega(BigDecimal valorFixo, BigDecimal valorPorKg, int prazo) {
        this.valorFixo = valorFixo;
        this.valorPorKg = valorPorKg;
        this.prazo = prazo;
    }

    public BigDecimal getValorFixo() {
        return valorFixo;
    }

    public BigDecimal getValorPorKg() {
        return valorPorKg;
    }

    public int getPrazo() {
        return prazo;
    }

    public static ModalidadeEntrega fromString(String valor) {
        try {
            return ModalidadeEntrega.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
