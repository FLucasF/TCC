package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;

public interface Cupom {
    BigDecimal calcularDesconto(ContextoCupom contexto);
    boolean aplicavel(ContextoCupom contexto);
}
