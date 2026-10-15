package com.loja.checkout.api;

import java.math.BigDecimal;

/**
 * Um item do carrinho, como o site envia.
 *
 * <p>Os campos são objetos (não primitivos) de propósito: assim conseguimos
 * distinguir "veio zero" de "não veio" (ausente), e os dois casos recusam o
 * pedido com {@code PEDIDO_INVALIDO}.
 */
public record ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
