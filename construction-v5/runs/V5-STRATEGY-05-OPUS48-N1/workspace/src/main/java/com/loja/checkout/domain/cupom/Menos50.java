package com.loja.checkout.domain.cupom;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Menos50 implements Cupom {

    private static final BigDecimal MINIMO = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(CupomContexto contexto) {
        return contexto.subtotal().compareTo(MINIMO) >= 0;
    }

    @Override
    public BigDecimal desconto(CupomContexto contexto) {
        return DESCONTO;
    }
}
