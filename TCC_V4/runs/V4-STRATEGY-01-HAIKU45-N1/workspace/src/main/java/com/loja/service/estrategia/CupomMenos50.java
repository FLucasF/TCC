package com.loja.service.estrategia;

import java.math.BigDecimal;

public class CupomMenos50 implements CalculoCupom {
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete) {
        return DESCONTO;
    }

    @Override
    public BigDecimal calcularDescontoFrete(BigDecimal frete) {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO) >= 0;
    }
}
