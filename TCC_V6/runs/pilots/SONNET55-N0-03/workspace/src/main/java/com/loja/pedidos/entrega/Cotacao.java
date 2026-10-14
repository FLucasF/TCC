package com.loja.pedidos.entrega;

import java.math.BigDecimal;

public record Cotacao(BigDecimal frete, int prazoDias) {
}
