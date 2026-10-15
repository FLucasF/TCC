package com.loja.checkout.web;

import java.math.BigDecimal;

/**
 * Um produto do carrinho, como o site envia. Os tipos são objeto (não
 * primitivo) de propósito: campo ausente chega como null e é tratado como
 * PEDIDO_INVALIDO.
 */
public record ItemPedido(
        String nome,
        BigDecimal precoUnitario,
        Integer quantidade,
        BigDecimal pesoKg) {
}
