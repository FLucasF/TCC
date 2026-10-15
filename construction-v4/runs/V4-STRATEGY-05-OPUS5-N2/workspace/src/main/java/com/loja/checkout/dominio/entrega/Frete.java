package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

/** Quanto custa e em quantos dias chega. */
public record Frete(BigDecimal valor, int prazoDias) {
}
