package br.tcc.checkout.dominio;

import java.math.BigDecimal;

/** Item do carrinho já validado. */
public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

	/** Preço × quantidade, sem arredondar. */
	public BigDecimal totalBruto() {
		return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
	}

	/** Peso × quantidade, sem arredondar. */
	public BigDecimal pesoTotal() {
		return pesoKg.multiply(BigDecimal.valueOf(quantidade));
	}
}
