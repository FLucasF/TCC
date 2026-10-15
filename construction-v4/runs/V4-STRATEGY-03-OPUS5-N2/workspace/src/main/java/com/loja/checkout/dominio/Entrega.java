package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** O que uma modalidade de entrega cobra e em quantos dias entrega. */
public record Entrega(BigDecimal frete, int prazoDias) {
}
