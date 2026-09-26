package com.loja.checkout.model;

import java.math.BigDecimal;

public enum Modalidade {
    ECONOMICA("ECONOMICA", new BigDecimal("12.00"), new BigDecimal("2.00"), 7),
    EXPRESSA("EXPRESSA", new BigDecimal("25.00"), new BigDecimal("4.50"), 2),
    RETIRADA_LOJA("RETIRADA_LOJA", BigDecimal.ZERO, BigDecimal.ZERO, 1),
    MOTOBOY("MOTOBOY", new BigDecimal("18.00"), BigDecimal.ZERO, 0);

    private final String codigo;
    private final BigDecimal taxaBase;
    private final BigDecimal taxaPorKg;
    private final Integer prazoDias;

    Modalidade(String codigo, BigDecimal taxaBase, BigDecimal taxaPorKg, Integer prazoDias) {
        this.codigo = codigo;
        this.taxaBase = taxaBase;
        this.taxaPorKg = taxaPorKg;
        this.prazoDias = prazoDias;
    }

    public String getCodigo() {
        return codigo;
    }

    public BigDecimal getTaxaBase() {
        return taxaBase;
    }

    public BigDecimal getTaxaPorKg() {
        return taxaPorKg;
    }

    public Integer getPrazoDias() {
        return prazoDias;
    }

    public static Modalidade fromCodigo(String codigo) {
        for (Modalidade m : Modalidade.values()) {
            if (m.codigo.equals(codigo)) {
                return m;
            }
        }
        return null;
    }
}
