package com.loja.checkout.dominio.clube;

import java.math.BigDecimal;

/** O que o nivel do clube rende neste pedido. */
public record Vantagens(BigDecimal creditoProximaCompra, BigDecimal freteDevido, boolean brinde) {
}
