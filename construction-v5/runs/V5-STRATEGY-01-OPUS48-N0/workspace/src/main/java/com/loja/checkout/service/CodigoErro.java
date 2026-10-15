package com.loja.checkout.service;

/**
 * Codigos de erro que o servico pode devolver quando nao da para calcular o
 * resumo da compra. A ordem de verificacao esta no {@link CheckoutService}.
 */
public enum CodigoErro {
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
