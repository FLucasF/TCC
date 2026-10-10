package com.loja.checkout.enums;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2),
    RETIRADA_LOJA(BigDecimal.ZERO, BigDecimal.ZERO, 1),
    MOTOBOY(new BigDecimal("18.00"), BigDecimal.ZERO, 0);

    private final BigDecimal basePrice;
    private final BigDecimal pricePerKg;
    private final int prazo;

    ModalidadeEntrega(BigDecimal basePrice, BigDecimal pricePerKg, int prazo) {
        this.basePrice = basePrice;
        this.pricePerKg = pricePerKg;
        this.prazo = prazo;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public BigDecimal getPricePerKg() {
        return pricePerKg;
    }

    public int getPrazo() {
        return prazo;
    }

    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return basePrice.add(pricePerKg.multiply(pesoTotal));
    }
}
