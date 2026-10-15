package br.tcc.checkout.cupom;

import java.math.BigDecimal;

public class FreteGratisCupom implements Cupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, BigDecimal frete) {
        return frete;
    }

    @Override
    public boolean aplicavel(BigDecimal subtotal) {
        return true;
    }
}
