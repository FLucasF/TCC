package com.loja.checkout.erro;

/** Os problemas que fazem o pedido ser recusado, na ordem em que sao conferidos. */
public enum Codigo {
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
