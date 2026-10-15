package com.loja.strategy.cupom;

import java.math.BigDecimal;

public class Menos50 implements Cupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete) {
        return new BigDecimal("50.00");
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(new BigDecimal("300.00")) >= 0;
    }
}
