package com.loja.checkout.cupom;

import java.math.BigDecimal;

/** Uma promocao. Cada cupom guarda sua condicao e sua propria conta. */
public interface Cupom {

    String codigo();

    /** Se o pedido cumpre a condicao do cupom. */
    boolean aplicavel(ContextoCupom contexto);

    /** Desconto concedido, arredondado para centavos. */
    BigDecimal desconto(ContextoCupom contexto);
}
