package br.tcc.checkout.cupom;

import java.math.BigDecimal;

public class Menos50Cupom implements Cupom {
    private static final BigDecimal VALOR_MINIMO = new BigDecimal("300.00");

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete) {
        return new BigDecimal("50.00");
    }

    @Override
    public boolean aplicavel(BigDecimal subtotal) {
        return subtotal.compareTo(VALOR_MINIMO) >= 0;
    }
}
