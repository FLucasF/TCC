package com.loja.checkout.web;

import java.math.BigDecimal;

/**
 * Item do carrinho como o site envia. Campos em objeto (não primitivo) para
 * distinguir "ausente" (null) de zero na validação do pedido.
 */
public record ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
