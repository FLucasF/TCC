package com.loja.checkout.domain.erro;

/**
 * Motivos pelos quais um pedido pode ser recusado. O codigo e exatamente o texto
 * que o site espera no corpo { "erro": "CODIGO" }.
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
