package com.loja.checkout.domain;

import java.math.BigDecimal;

/** Um item ja validado do carrinho. */
public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal totalLinha() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal pesoLinha() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }

    /** Quantas unidades deste item saem de graca na regra "leve 3 pague 2". */
    public int unidadesGratis3por2() {
        return quantidade / 3;
    }
}
