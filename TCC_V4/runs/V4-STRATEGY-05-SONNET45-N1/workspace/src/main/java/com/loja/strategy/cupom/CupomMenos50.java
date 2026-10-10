package com.loja.strategy.cupom;

import com.loja.dto.CheckoutRequest;
import java.math.BigDecimal;

public class CupomMenos50 implements Cupom {
    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO = new BigDecimal("300.00");

    @Override
    public boolean isAplicavel(BigDecimal subtotalProdutos, CheckoutRequest request) {
        return subtotalProdutos.compareTo(MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, CheckoutRequest request) {
        return DESCONTO;
    }
}
