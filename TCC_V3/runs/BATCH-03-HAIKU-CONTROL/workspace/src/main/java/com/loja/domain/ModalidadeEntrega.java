package com.loja.domain;

import java.math.BigDecimal;
import java.util.Optional;

public enum ModalidadeEntrega {
    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7, null),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2, null),
    RETIRADA_LOJA(new BigDecimal("0.00"), new BigDecimal("0.00"), 1, null),
    MOTOBOY(new BigDecimal("18.00"), new BigDecimal("0.00"), 0, new BigDecimal("5.00"));

    private final BigDecimal taxa;
    private final BigDecimal taxaPorKg;
    private final int prazo;
    private final BigDecimal pesoMaximo;

    ModalidadeEntrega(BigDecimal taxa, BigDecimal taxaPorKg, int prazo, BigDecimal pesoMaximo) {
        this.taxa = taxa;
        this.taxaPorKg = taxaPorKg;
        this.prazo = prazo;
        this.pesoMaximo = pesoMaximo;
    }

    public BigDecimal getTaxa() {
        return taxa;
    }

    public BigDecimal getTaxaPorKg() {
        return taxaPorKg;
    }

    public int getPrazo() {
        return prazo;
    }

    public Optional<BigDecimal> getPesoMaximo() {
        return Optional.ofNullable(pesoMaximo);
    }

    public static Optional<ModalidadeEntrega> fromString(String value) {
        try {
            return Optional.of(ModalidadeEntrega.valueOf(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
