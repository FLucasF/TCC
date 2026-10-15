package com.loja.checkout.web;

import java.math.BigDecimal;

/**
 * Um item como o site envia. Campos podem vir nulos; a validacao decide.
 */
public record ItemRequest(
        String nome,
        BigDecimal precoUnitario,
        Integer quantidade,
        BigDecimal pesoKg) {
}
