package com.loja.checkout.domain;

public enum ModalidadeEntrega {
    ECONOMICA(12.0, 2.0, 7),
    EXPRESSA(25.0, 4.5, 2),
    RETIRADA_LOJA(0.0, 0.0, 1),
    MOTOBOY(18.0, 0.0, 0);

    private final Double valorBase;
    private final Double valorPorKg;
    private final Integer prazo;

    ModalidadeEntrega(Double valorBase, Double valorPorKg, Integer prazo) {
        this.valorBase = valorBase;
        this.valorPorKg = valorPorKg;
        this.prazo = prazo;
    }

    public Double getValorBase() {
        return valorBase;
    }

    public Double getValorPorKg() {
        return valorPorKg;
    }

    public Integer getPrazo() {
        return prazo;
    }

    public Double calcularFrete(Double pesoTotal) {
        return valorBase + (valorPorKg * pesoTotal);
    }

    public static ModalidadeEntrega parse(String valor) {
        if (valor == null) return null;
        try {
            return ModalidadeEntrega.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
