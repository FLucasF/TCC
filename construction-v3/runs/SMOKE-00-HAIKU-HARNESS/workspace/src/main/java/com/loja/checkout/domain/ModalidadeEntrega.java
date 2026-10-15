package com.loja.checkout.domain;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA("ECONOMICA", new BigDecimal("12.00"), new BigDecimal("2.00"), 7),
    EXPRESSA("EXPRESSA", new BigDecimal("25.00"), new BigDecimal("4.50"), 2),
    RETIRADA_LOJA("RETIRADA_LOJA", BigDecimal.ZERO, BigDecimal.ZERO, 1),
    MOTOBOY("MOTOBOY", new BigDecimal("18.00"), BigDecimal.ZERO, 0);

    private final String codigo;
    private final BigDecimal taxaBase;
    private final BigDecimal taxaPorKg;
    private final int prazoEntregaDias;

    ModalidadeEntrega(String codigo, BigDecimal taxaBase, BigDecimal taxaPorKg, int prazoEntregaDias) {
        this.codigo = codigo;
        this.taxaBase = taxaBase;
        this.taxaPorKg = taxaPorKg;
        this.prazoEntregaDias = prazoEntregaDias;
    }

    public static ModalidadeEntrega fromCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (ModalidadeEntrega modalidade : values()) {
            if (modalidade.codigo.equals(codigo)) {
                return modalidade;
            }
        }
        return null;
    }

    public BigDecimal getTaxaBase() {
        return taxaBase;
    }

    public BigDecimal getTaxaPorKg() {
        return taxaPorKg;
    }

    public int getPrazoEntregaDias() {
        return prazoEntregaDias;
    }
}
