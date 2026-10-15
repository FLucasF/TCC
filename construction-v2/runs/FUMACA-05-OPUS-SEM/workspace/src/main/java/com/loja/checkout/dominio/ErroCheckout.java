package com.loja.checkout.dominio;

/** Codigos de erro devolvidos ao site, na forma {"erro": "CODIGO"}. */
public enum ErroCheckout {
    PEDIDO_INVALIDO,
    MODALIDADE_INVALIDA,
    MODALIDADE_INDISPONIVEL,
    CUPOM_INVALIDO,
    CUPOM_NAO_APLICAVEL,
    FORMA_PAGAMENTO_INVALIDA,
    FORMA_PAGAMENTO_INDISPONIVEL,
    PARCELAMENTO_INVALIDO
}
