package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Resultado da modalidade de entrega escolhida. */
public record Entrega(BigDecimal frete, int prazoDias) {
}
