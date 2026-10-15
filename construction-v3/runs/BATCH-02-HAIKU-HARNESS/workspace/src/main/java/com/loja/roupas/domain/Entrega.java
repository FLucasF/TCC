package com.loja.roupas.domain;

import java.math.BigDecimal;

public enum Entrega {
    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7, null) {
        @Override
        public BigDecimal calcularFrete(Double pesoKg) {
            BigDecimal peso = new BigDecimal(pesoKg.toString());
            return custaFixa.add(costaPorKg.multiply(peso));
        }
    },
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2, null) {
        @Override
        public BigDecimal calcularFrete(Double pesoKg) {
            BigDecimal peso = new BigDecimal(pesoKg.toString());
            return custaFixa.add(costaPorKg.multiply(peso));
        }
    },
    RETIRADA_LOJA(new BigDecimal("0.00"), new BigDecimal("0.00"), 1, null) {
        @Override
        public BigDecimal calcularFrete(Double pesoKg) {
            return new BigDecimal("0.00");
        }
    },
    MOTOBOY(new BigDecimal("18.00"), new BigDecimal("0.00"), 0, 5.0) {
        @Override
        public BigDecimal calcularFrete(Double pesoKg) {
            return new BigDecimal("18.00");
        }
    };

    protected final BigDecimal custaFixa;
    protected final BigDecimal costaPorKg;
    protected final Integer prazo;
    protected final Double pesoMaximo;

    Entrega(BigDecimal custaFixa, BigDecimal costaPorKg, Integer prazo, Double pesoMaximo) {
        this.custaFixa = custaFixa;
        this.costaPorKg = costaPorKg;
        this.prazo = prazo;
        this.pesoMaximo = pesoMaximo;
    }

    public Integer getPrazo() {
        return prazo;
    }

    public boolean estaDisponivel(Double pesoTotal) {
        if (pesoMaximo == null) {
            return true;
        }
        return pesoTotal <= pesoMaximo;
    }

    public abstract BigDecimal calcularFrete(Double pesoKg);
}
