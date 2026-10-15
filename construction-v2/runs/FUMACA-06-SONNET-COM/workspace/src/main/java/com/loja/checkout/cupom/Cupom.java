package com.loja.checkout.cupom;

import java.math.BigDecimal;

public interface Cupom {

    String codigo();

    boolean aplicavel(ContextoDesconto contexto);

    BigDecimal desconto(ContextoDesconto contexto);
}
