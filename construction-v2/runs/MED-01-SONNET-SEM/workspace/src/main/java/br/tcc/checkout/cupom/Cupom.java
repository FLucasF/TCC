package br.tcc.checkout.cupom;

import br.tcc.checkout.dominio.DadosPedido;

import java.math.BigDecimal;

public interface Cupom {

	String getCodigo();

	boolean isAplicavel(DadosPedido pedido);

	/**
	 * @param frete frete já calculado (arredondado), necessário para cupons
	 *              como o FRETEGRATIS, cujo desconto depende do valor do frete.
	 */
	BigDecimal calcularDesconto(DadosPedido pedido, BigDecimal frete);
}
