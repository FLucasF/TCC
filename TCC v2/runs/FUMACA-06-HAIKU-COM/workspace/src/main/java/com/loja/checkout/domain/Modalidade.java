package com.loja.checkout.domain;

import java.math.BigDecimal;

public enum Modalidade {
    ECONOMICA("ECONOMICA", new BigDecimal("12.00"), new BigDecimal("2.00"), 7),
    EXPRESSA("EXPRESSA", new BigDecimal("25.00"), new BigDecimal("4.50"), 2),
    RETIRADA_LOJA("RETIRADA_LOJA", BigDecimal.ZERO, BigDecimal.ZERO, 1),
    MOTOBOY("MOTOBOY", new BigDecimal("18.00"), BigDecimal.ZERO, 0);

    private final String codigo;
    private final BigDecimal taxaBase;
    private final BigDecimal taxaPorKg;
    private final int prazo;

    Modalidade(String codigo, BigDecimal taxaBase, BigDecimal taxaPorKg, int prazo) {
        this.codigo = codigo;
        this.taxaBase = taxaBase;
        this.taxaPorKg = taxaPorKg;
        this.prazo = prazo;
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

    public int getPrazo() {
        return prazo;
    }

    public static Modalidade fromCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (Modalidade m : values()) {
            if (m.codigo.equals(codigo)) {
                return m;
            }
        }
        return null;
    }

    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        if (this == MOTOBOY) {
            return taxaBase;
        }
        if (this == RETIRADA_LOJA) {
            return BigDecimal.ZERO;
        }
        return taxaBase.add(taxaPorKg.multiply(pesoTotal));
    }

    public boolean isDisponivel(BigDecimal pesoTotal) {
        if (this == MOTOBOY) {
            return pesoTotal.compareTo(new BigDecimal("5")) <= 0;
        }
        return true;
    }
}
