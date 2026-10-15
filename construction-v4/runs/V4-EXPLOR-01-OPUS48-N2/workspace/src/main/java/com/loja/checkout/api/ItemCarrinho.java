package com.loja.checkout.api;

import java.math.BigDecimal;

/**
 * Um produto do carrinho. Preço e peso vêm como número com ponto; quantidade
 * como inteiro. Os tipos são objeto (não primitivo) para distinguir "ausente".
 */
public record ItemCarrinho(
        String nome,
        BigDecimal precoUnitario,
        Integer quantidade,
        BigDecimal pesoKg) {
}
