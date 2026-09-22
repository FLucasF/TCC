package com.loja.checkout.cupom;

import java.math.BigDecimal;

/** Uma promocao: quando vale e quanto desconta. */
public interface Cupom {

    String codigo();

    BigDecimal desconto(BaseDoCupom base);

    default boolean aplicavel(BaseDoCupom base) {
        return true;
    }
}
