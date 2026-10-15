package com.loja.checkout.domain;

/**
 * Os motivos pelos quais um pedido pode ser recusado. O nome de cada constante
 * é exatamente o código devolvido ao site.
 */
public enum ErroCheckout {
    PEDIDO_INVALIDO,
    NIVEL_CLUBE_INVALIDO,
    REGIAO_INVALIDA,
    MODALIDADE_INVALIDA,
    MODALIDADE_INDISPONIVEL,
    CUPOM_INVALIDO,
    CUPOM_NAO_APLICAVEL,
    FORMA_PAGAMENTO_INVALIDA,
    PARCELAMENTO_INVALIDO,
    FORMA_PAGAMENTO_INDISPONIVEL
}
