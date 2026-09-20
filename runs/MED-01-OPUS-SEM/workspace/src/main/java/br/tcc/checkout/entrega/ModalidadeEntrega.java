package br.tcc.checkout.entrega;

import java.math.BigDecimal;

import br.tcc.checkout.dominio.Pedido;

/**
 * Uma opção de entrega. Cada transportadora nova entra como uma implementação
 * anotada com {@code @Component}: o catálogo a descobre sozinho, sem alterar o
 * cálculo do resumo.
 */
public interface ModalidadeEntrega {

	/** Código usado pelo site no campo {@code modalidadeEntrega}. */
	String codigo();

	/** Prazo prometido ao cliente, em dias. */
	int prazoDias();

	/** Frete cobrado, já arredondado em centavos. */
	BigDecimal calcularFrete(Pedido pedido);

	/**
	 * Se a modalidade atende este pedido. Quando devolve {@code false} o cliente
	 * recebe {@code MODALIDADE_INDISPONIVEL}.
	 */
	default boolean atende(Pedido pedido) {
		return true;
	}
}
