package com.loja.checkout.cupom;

import java.math.BigDecimal;

public class CupomMenos50 implements Cupom {

    private static final BigDecimal MINIMO = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    @Override
    public boolean isAplicavel(CupomContexto ctx) {
        return ctx.subtotalProdutos().compareTo(MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(CupomContexto ctx) {
        return DESCONTO;
    }
}
