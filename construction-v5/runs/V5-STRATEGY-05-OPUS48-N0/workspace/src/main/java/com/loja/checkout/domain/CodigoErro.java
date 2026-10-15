package com.loja.checkout.domain;

/**
 * Códigos de erro que o serviço pode devolver quando recusa um pedido.
 *
 * <p>A ordem em que são verificados está definida em
 * {@link com.loja.checkout.service.CheckoutService}; aqui ficam apenas os códigos.
 */
public enum CodigoErro {

    /** Carrinho vazio, ou item com preço, quantidade ou peso zero/negativo/ausente. */
    PEDIDO_INVALIDO,

    /** Nível do clube que não existe ou não foi informado. */
    NIVEL_CLUBE_INVALIDO,

    /** Região que não existe ou não foi informada. */
    REGIAO_INVALIDA,

    /** Opção de entrega que não existe ou não foi informada. */
    MODALIDADE_INVALIDA,

    /** Opção de entrega existe, mas não atende o pedido (ex.: motoboy acima de 5 kg). */
    MODALIDADE_INDISPONIVEL,

    /** Cupom informado que não existe. */
    CUPOM_INVALIDO,

    /** Cupom existe, mas o pedido não cumpre a condição (ex.: MENOS50 abaixo de R$ 300,00). */
    CUPOM_NAO_APLICAVEL,

    /** Forma de pagamento que não existe ou não foi informada. */
    FORMA_PAGAMENTO_INVALIDA,

    /** Número de parcelas não permitido para a forma de pagamento. */
    PARCELAMENTO_INVALIDO,

    /** Forma de pagamento existe, mas não atende o pedido (ex.: boleto acima de R$ 1.000,00). */
    FORMA_PAGAMENTO_INDISPONIVEL
}
