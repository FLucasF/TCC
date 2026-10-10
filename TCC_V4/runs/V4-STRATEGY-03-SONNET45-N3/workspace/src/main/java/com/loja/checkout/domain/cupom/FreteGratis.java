package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;

public class FreteGratis implements Cupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete) {
        return frete;
    }

    @Override
    public boolean verificarAplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
