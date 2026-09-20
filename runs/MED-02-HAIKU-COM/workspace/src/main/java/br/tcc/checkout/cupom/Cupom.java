package br.tcc.checkout.cupom;

import java.math.BigDecimal;

public interface Cupom {
    BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete);

    boolean aplicavel(BigDecimal subtotal);
}
