package br.tcc.checkout.dominio;

import java.math.BigDecimal;

/** Um item do carrinho, já validado. */
public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

	public BigDecimal total() {
		return Dinheiro.centavos(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
	}

	public BigDecimal peso() {
		return pesoKg.multiply(BigDecimal.valueOf(quantidade));
	}
}
