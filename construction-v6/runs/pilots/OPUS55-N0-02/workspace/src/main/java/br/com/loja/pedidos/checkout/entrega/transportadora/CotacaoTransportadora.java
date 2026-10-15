package br.com.loja.pedidos.checkout.entrega.transportadora;

import java.math.BigDecimal;

public record CotacaoTransportadora(BigDecimal frete, int prazoDias) {
}
