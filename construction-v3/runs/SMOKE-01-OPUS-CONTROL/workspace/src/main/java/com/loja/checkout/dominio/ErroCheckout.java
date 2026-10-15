package com.loja.checkout.dominio;

/** Codigos de erro devolvidos ao site. */
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
