package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Um item do carrinho, já validado.
 *
 * @param nome          nome do produto
 * @param precoUnitario preço de uma unidade
 * @param quantidade    quantas unidades
 * @param pesoKg        peso de uma unidade, em kg
 */
public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Valor deste item: preço × quantidade. */
    public BigDecimal total() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    /** Peso deste item: peso unitário × quantidade. */
    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
