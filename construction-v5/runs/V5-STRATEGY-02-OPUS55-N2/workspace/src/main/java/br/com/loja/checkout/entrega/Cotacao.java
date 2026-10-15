package br.com.loja.checkout.entrega;

import java.math.BigDecimal;

public record Cotacao(BigDecimal frete, int prazoDias) {
}
