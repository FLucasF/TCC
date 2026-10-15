package com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Cada cupom tem sua propria regra de quando vale e de quanto desconta. Como
 * o marketing cria promocao nova com frequencia, um cupom novo so precisa de
 * uma implementacao nova desta interface.
 */
public interface Cupom {

    String codigo();

    boolean aplicavel(ContextoCupom contexto);

    BigDecimal calcularDesconto(ContextoCupom contexto);
}
