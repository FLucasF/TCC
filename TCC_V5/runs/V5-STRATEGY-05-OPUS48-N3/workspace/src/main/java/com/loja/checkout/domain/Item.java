package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Um item já validado do carrinho. Preço e peso por unidade, quantidade inteira.
 */
public record Item(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Valor da linha: preço unitário × quantidade, sem arredondar (o subtotal
     *  é arredondado uma vez, depois de somar todas as linhas). */
    public BigDecimal totalLinha() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    /** Peso da linha: peso unitário × quantidade, sem arredondar. */
    public BigDecimal pesoLinha() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
