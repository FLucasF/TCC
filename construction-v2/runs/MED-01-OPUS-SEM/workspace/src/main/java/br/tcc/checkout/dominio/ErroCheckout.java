package br.tcc.checkout.dominio;

/** Códigos de erro devolvidos pelo serviço de resumo de checkout. */
public enum ErroCheckout {
	PEDIDO_INVALIDO,
	MODALIDADE_INVALIDA,
	MODALIDADE_INDISPONIVEL,
	CUPOM_INVALIDO,
	CUPOM_NAO_APLICAVEL,
	FORMA_PAGAMENTO_INVALIDA,
	PARCELAMENTO_INVALIDO,
	FORMA_PAGAMENTO_INDISPONIVEL
}
