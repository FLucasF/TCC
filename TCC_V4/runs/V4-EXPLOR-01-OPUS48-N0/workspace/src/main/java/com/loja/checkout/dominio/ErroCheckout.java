package com.loja.checkout.dominio;

/**
 * Motivos pelos quais um pedido pode ser recusado.
 *
 * <p>A ordem de verificação segue a ordem declarada aqui (e documentada no
 * contrato do serviço): o primeiro problema encontrado é o devolvido.
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
