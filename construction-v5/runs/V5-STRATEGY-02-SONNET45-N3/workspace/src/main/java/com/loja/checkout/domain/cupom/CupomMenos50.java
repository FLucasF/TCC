package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;

public class CupomMenos50 implements Cupom {

    private static final BigDecimal VALOR_MINIMO = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        return DESCONTO;
    }

    @Override
    public boolean eAplicavel(ContextoCupom contexto) {
        return contexto.subtotalProdutos().compareTo(VALOR_MINIMO) >= 0;
    }
}
