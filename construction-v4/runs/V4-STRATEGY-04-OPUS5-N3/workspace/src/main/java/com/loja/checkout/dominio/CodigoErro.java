package com.loja.checkout.dominio;

/**
 * Motivos pelos quais o pedido é recusado. A ordem das constantes é a precedência combinada
 * com a loja: quando há mais de um problema, vale o primeiro desta lista.
 *
 * <ol>
 * <li>{@link #PEDIDO_INVALIDO} — carrinho vazio, ou item com preço, quantidade ou peso
 *     zero, negativo ou ausente</li>
 * <li>{@link #NIVEL_CLUBE_INVALIDO} — nível do clube que não existe ou não informado</li>
 * <li>{@link #REGIAO_INVALIDA} — região que não existe ou não informada</li>
 * <li>{@link #MODALIDADE_INVALIDA} — entrega que não existe ou não informada</li>
 * <li>{@link #MODALIDADE_INDISPONIVEL} — entrega existe, mas não atende o pedido</li>
 * <li>{@link #CUPOM_INVALIDO} — cupom informado que não existe</li>
 * <li>{@link #CUPOM_NAO_APLICAVEL} — cupom existe, mas o pedido não cumpre a condição</li>
 * <li>{@link #FORMA_PAGAMENTO_INVALIDA} — pagamento que não existe ou não informado</li>
 * <li>{@link #PARCELAMENTO_INVALIDO} — número de parcelas não permitido para o pagamento</li>
 * <li>{@link #FORMA_PAGAMENTO_INDISPONIVEL} — pagamento existe, mas não atende o pedido</li>
 * </ol>
 *
 * É {@link CalculadoraResumo} que confere nesta ordem; os testes de precedência travam isso.
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
