package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;

/**
 * Uma promocao. Cada cupom tem seu codigo, sua condicao para valer e seu jeito
 * de calcular o desconto. Para criar uma promocao nova, basta uma nova classe
 * que implemente esta interface e seja um {@code @Component}.
 */
public interface Cupom {

    /** Codigo do cupom, sempre em maiusculas, ex.: "BEMVINDO10". */
    String codigo();

    /** O pedido cumpre a condicao do cupom? Ex.: MENOS50 exige R$ 300 em produtos. */
    boolean aplicavel(ContextoCupom contexto);

    /** Valor do desconto (ainda sem arredondar). */
    BigDecimal calcularDesconto(ContextoCupom contexto);
}
