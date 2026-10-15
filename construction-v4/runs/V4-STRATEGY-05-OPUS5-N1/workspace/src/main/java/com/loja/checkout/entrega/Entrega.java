package com.loja.checkout.entrega;

import java.math.BigDecimal;

/** O que a modalidade escolhida cobra e em quantos dias entrega. */
public record Entrega(BigDecimal frete, int prazoDias) {
}
