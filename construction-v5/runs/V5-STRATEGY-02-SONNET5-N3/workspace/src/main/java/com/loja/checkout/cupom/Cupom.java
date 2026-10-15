package com.loja.checkout.cupom;

import java.math.BigDecimal;

public interface Cupom {

    String codigo();

    boolean aplicavel(CupomContexto contexto);

    BigDecimal desconto(CupomContexto contexto);
}
