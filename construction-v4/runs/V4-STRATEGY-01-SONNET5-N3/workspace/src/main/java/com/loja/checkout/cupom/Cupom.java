package com.loja.checkout.cupom;

import java.math.BigDecimal;

public interface Cupom {

    boolean aplicavel(ContextoCupom contexto);

    BigDecimal calcularDesconto(ContextoCupom contexto);
}
