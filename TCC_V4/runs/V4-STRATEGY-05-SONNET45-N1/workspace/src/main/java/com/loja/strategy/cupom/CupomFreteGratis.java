package com.loja.strategy.cupom;

import com.loja.dto.CheckoutRequest;
import java.math.BigDecimal;

public class CupomFreteGratis implements Cupom {
    @Override
    public boolean isAplicavel(BigDecimal subtotalProdutos, CheckoutRequest request) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, CheckoutRequest request) {
        return frete;
    }
}
