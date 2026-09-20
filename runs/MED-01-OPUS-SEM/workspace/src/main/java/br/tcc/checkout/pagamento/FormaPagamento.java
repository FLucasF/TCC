package br.tcc.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento aceita na loja. Cada nova forma entra como uma
 * implementação anotada com {@code @Component}.
 */
public interface FormaPagamento {

	/** Código usado pelo site no campo {@code formaPagamento}. */
	String codigo();

	/** Se aceita esse número de parcelas; caso contrário, {@code PARCELAMENTO_INVALIDO}. */
	boolean aceitaParcelas(int parcelas);

	/**
	 * Se a forma atende um pedido desse valor. Quando devolve {@code false} o
	 * cliente recebe {@code FORMA_PAGAMENTO_INDISPONIVEL}.
	 */
	default boolean atende(BigDecimal totalPedido) {
		return true;
	}

	/** Aplica desconto, tarifa ou juros sobre o total do pedido. */
	ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas);
}
