package com.loja.checkout.web;

import java.math.BigDecimal;

/**
 * Item do carrinho como o site envia. Campos de valor são objetos (podem ser
 * nulos) para que "ausente" seja detectado como pedido inválido.
 */
public record ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
