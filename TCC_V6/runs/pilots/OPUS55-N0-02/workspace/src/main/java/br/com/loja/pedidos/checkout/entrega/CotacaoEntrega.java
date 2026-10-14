package br.com.loja.pedidos.checkout.entrega;

import java.math.BigDecimal;

public record CotacaoEntrega(BigDecimal frete, int prazoDias) {
}
