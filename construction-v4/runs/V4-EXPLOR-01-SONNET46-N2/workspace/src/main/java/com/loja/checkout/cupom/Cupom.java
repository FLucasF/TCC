package com.loja.checkout.cupom;

import java.math.BigDecimal;

public interface Cupom {
    boolean isAplicavel(CupomContexto ctx);
    BigDecimal calcularDesconto(CupomContexto ctx);
}
