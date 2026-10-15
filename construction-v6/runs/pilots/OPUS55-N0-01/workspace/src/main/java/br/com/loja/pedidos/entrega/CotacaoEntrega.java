package br.com.loja.pedidos.entrega;

import java.math.BigDecimal;

public record CotacaoEntrega(BigDecimal frete, int prazoDias) {
}
