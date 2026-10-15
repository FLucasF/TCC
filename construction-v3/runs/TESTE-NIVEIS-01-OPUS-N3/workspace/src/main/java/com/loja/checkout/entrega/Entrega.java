package com.loja.checkout.entrega;

import java.math.BigDecimal;

/** O que uma modalidade apura para o pedido: quanto custa e em quantos dias. */
public record Entrega(BigDecimal frete, int prazoDias) {
}
