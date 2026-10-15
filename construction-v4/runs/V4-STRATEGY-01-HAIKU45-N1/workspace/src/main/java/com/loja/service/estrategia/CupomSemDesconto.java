package com.loja.service.estrategia;

import java.math.BigDecimal;

public class CupomSemDesconto implements CalculoCupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calcularDescontoFrete(BigDecimal frete) {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
