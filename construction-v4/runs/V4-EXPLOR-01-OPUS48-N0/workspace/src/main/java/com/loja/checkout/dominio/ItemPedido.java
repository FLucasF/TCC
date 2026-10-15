package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Um item do carrinho já validado, pronto para o cálculo.
 *
 * @param nome           nome do produto
 * @param precoUnitario  preço de uma unidade
 * @param quantidade     quantidade pedida
 * @param pesoKg         peso de uma unidade, em kg
 */
public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    /** Valor deste item: preço unitário × quantidade. */
    public BigDecimal totalItem() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    /** Peso deste item: peso unitário × quantidade. */
    public BigDecimal pesoTotal() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
