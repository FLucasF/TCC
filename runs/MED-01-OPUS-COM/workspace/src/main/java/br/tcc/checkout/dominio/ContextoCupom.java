package br.tcc.checkout.dominio;

import java.math.BigDecimal;

/** Tudo que um cupom pode olhar para decidir se vale e quanto desconta. */
public record ContextoCupom(Pedido pedido, ModalidadeEntrega modalidadeEntrega, BigDecimal frete) {

	public BigDecimal subtotalProdutos() {
		return pedido.subtotalProdutos();
	}
}
