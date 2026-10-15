package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

/** O que uma modalidade de entrega cobra e em quantos dias entrega. */
public record Entrega(BigDecimal valor, int prazoDias) {
}
