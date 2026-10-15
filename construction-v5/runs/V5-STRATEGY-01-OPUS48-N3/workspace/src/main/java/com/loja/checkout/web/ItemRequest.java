package com.loja.checkout.web;

import java.math.BigDecimal;

/**
 * Um produto do carrinho como o site envia. Campos podem vir nulos; a validação
 * fica na calculadora.
 */
public record ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
