package com.loja.checkout.web;

import java.math.BigDecimal;

/**
 * Um produto do carrinho como chega do site. Os campos podem vir nulos; a
 * validação acontece no serviço para devolver o código de erro certo.
 */
public record ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
