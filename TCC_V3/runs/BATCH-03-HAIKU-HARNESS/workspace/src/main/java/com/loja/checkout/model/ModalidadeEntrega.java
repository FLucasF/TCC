package com.loja.checkout.model;

import java.util.Map;

public enum ModalidadeEntrega {
    ECONOMICA(12.00, 2.00, 7),
    EXPRESSA(25.00, 4.50, 2),
    RETIRADA_LOJA(0.00, 0.00, 1),
    MOTOBOY(18.00, 0.00, 0);

    private final Double custoFixo;
    private final Double custoKg;
    private final Integer prazo;

    ModalidadeEntrega(Double custoFixo, Double custoKg, Integer prazo) {
        this.custoFixo = custoFixo;
        this.custoKg = custoKg;
        this.prazo = prazo;
    }

    public Double getCustoFixo() {
        return custoFixo;
    }

    public Double getCustoKg() {
        return custoKg;
    }

    public Integer getPrazo() {
        return prazo;
    }

    public Double calcularFrete(Double pesoTotal) {
        return custoFixo + (custoKg * pesoTotal);
    }

    public static ModalidadeEntrega fromString(String modalidade) {
        if (modalidade == null) {
            return null;
        }
        try {
            return ModalidadeEntrega.valueOf(modalidade);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
