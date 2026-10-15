package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;

public class BemVindo10 implements Cupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete) {
        return subtotalProdutos.multiply(new BigDecimal("0.10"));
    }

    @Override
    public boolean verificarAplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
