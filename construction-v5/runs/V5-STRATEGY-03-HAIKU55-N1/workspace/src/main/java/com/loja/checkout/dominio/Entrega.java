package com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum Entrega {

    ECONOMICA("12.00", "2.00", 7),
    EXPRESSA("25.00", "4.50", 2),
    RETIRADA_LOJA("0.00", "0.00", 1),
    MOTOBOY("18.00", "0.00", 0) {
        @Override
        public boolean atende(BigDecimal pesoKg) {
            return pesoKg.compareTo(new BigDecimal("5")) <= 0;
        }
    };

    private final BigDecimal taxaFixa;
    private final BigDecimal taxaPorKg;
    private final int prazoDias;

    Entrega(String taxaFixa, String taxaPorKg, int prazoDias) {
        this.taxaFixa = new BigDecimal(taxaFixa);
        this.taxaPorKg = new BigDecimal(taxaPorKg);
        this.prazoDias = prazoDias;
    }

    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.centavos(taxaFixa.add(taxaPorKg.multiply(pesoKg)));
    }

    public int prazoDias() {
        return prazoDias;
    }

    public boolean atende(BigDecimal pesoKg) {
        return true;
    }
}
