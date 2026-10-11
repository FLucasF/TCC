package com.loja.checkout.web;

import java.math.BigDecimal;

/** Item como o site envia. Campos anulaveis para permitir validar ausencias. */
public record ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
