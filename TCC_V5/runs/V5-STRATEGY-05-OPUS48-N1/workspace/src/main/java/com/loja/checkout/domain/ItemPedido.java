package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Um produto do carrinho, ja validado. O peso e o preco entram sem
 * arredondar; quem arredonda sao as etapas do calculo.
 */
public record ItemPedido(BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal totalItem() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal pesoTotalItem() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
