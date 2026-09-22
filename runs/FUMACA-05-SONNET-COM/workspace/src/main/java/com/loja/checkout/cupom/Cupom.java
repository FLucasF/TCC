package com.loja.checkout.cupom;

import java.math.BigDecimal;

public interface Cupom {

    String codigo();

    boolean aplicavel(ContextoCupom contexto);

    BigDecimal calcularDesconto(ContextoCupom contexto);
}
