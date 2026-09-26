package br.tcc.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/** Carrinho validado, com os valores que não dependem de entrega, cupom ou pagamento. */
public record Pedido(List<ItemPedido> itens) {

	public Pedido {
		itens = List.copyOf(itens);
	}

	/** Soma dos itens (preço × quantidade), arredondada em centavos. */
	public BigDecimal subtotalProdutos() {
		return Dinheiro.arredondar(itens.stream()
				.map(ItemPedido::totalBruto)
				.reduce(BigDecimal.ZERO, BigDecimal::add));
	}

	/** Peso total do pedido em kg, sem arredondamento. */
	public BigDecimal pesoTotalKg() {
		return itens.stream()
				.map(ItemPedido::pesoTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
