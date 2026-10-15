package com.loja.checkout.dominio.cupom;

import java.math.BigDecimal;

public interface Cupom {

    String codigo();

    boolean aplicavel(ContextoCupom contexto);

    BigDecimal calcularDesconto(ContextoCupom contexto);
}
