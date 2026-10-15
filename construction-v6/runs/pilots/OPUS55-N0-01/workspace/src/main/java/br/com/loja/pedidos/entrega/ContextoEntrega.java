package br.com.loja.pedidos.entrega;

import java.math.BigDecimal;

import br.com.loja.pedidos.checkout.Regiao;

/** O que uma opção de entrega precisa saber do pedido para cotar. */
public record ContextoEntrega(String cep, Regiao regiao, BigDecimal pesoKg, BigDecimal subtotalProdutos) {
}
