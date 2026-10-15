package br.tcc.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** O carrinho do cliente: os itens e o que se deriva deles. */
public record Pedido(List<ItemPedido> itens) {

	public Pedido {
		itens = List.copyOf(itens);
	}

	public BigDecimal subtotalProdutos() {
		return Dinheiro.centavos(itens.stream()
				.map(ItemPedido::total)
				.reduce(BigDecimal.ZERO, BigDecimal::add));
	}

	/** Peso do pedido em kg, sem arredondar. */
	public BigDecimal pesoKg() {
		return itens.stream()
				.map(ItemPedido::peso)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
