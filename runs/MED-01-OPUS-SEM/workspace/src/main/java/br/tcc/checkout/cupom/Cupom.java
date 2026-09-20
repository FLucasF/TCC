package br.tcc.checkout.cupom;

import java.math.BigDecimal;

/**
 * Uma promoção do marketing. Cada cupom novo entra como uma implementação
 * anotada com {@code @Component} e passa a valer sem mexer no cálculo do resumo.
 */
public interface Cupom {

	/** Código digitado pelo cliente, sempre em maiúsculas. */
	String codigo();

	/** Desconto concedido, já arredondado em centavos. */
	BigDecimal calcularDesconto(ContextoCupom contexto);

	/**
	 * Se o pedido cumpre a condição da promoção. Quando devolve {@code false} o
	 * cliente recebe {@code CUPOM_NAO_APLICAVEL}.
	 */
	default boolean aplicavel(ContextoCupom contexto) {
		return true;
	}
}
