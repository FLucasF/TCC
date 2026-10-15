package com.loja.checkout.service.cupom;

import java.math.BigDecimal;

public interface Cupom {

    String getCodigo();

    boolean isAplicavel(ContextoCupom contexto);

    BigDecimal calcularDesconto(ContextoCupom contexto);
}
