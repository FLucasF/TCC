package com.loja.checkout.dominio.cupom;

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
    public boolean aplicavel(ContextoCupom ctx) {
        return ctx.subtotalProdutos().compareTo(MINIMO) >= 0;
    }

    @Override
    public BigDecimal desconto(ContextoCupom ctx) {
        return DESCONTO;
    }
}
