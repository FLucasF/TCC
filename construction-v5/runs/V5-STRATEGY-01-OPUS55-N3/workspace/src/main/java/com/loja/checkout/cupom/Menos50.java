package com.loja.checkout.cupom;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Menos50 implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal COMPRA_MINIMA = new BigDecimal("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return contexto.carrinho().subtotal().compareTo(COMPRA_MINIMA) >= 0;
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return DESCONTO;
    }
}
