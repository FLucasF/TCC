package com.loja.checkout.cupom;

import java.math.BigDecimal;

public interface Cupom {

    String codigo();

    boolean aplicavel(ContextoCupom ctx);

    BigDecimal calcularDesconto(ContextoCupom ctx);
}
