package com.loja.strategy.cupom;

import com.loja.dto.CheckoutRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CupomBemVindo10 implements Cupom {
    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public boolean isAplicavel(BigDecimal subtotalProdutos, CheckoutRequest request) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, CheckoutRequest request) {
        return subtotalProdutos.multiply(PERCENTUAL)
                .setScale(2, RoundingMode.HALF_EVEN);
    }
}
