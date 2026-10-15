package com.loja.checkout.strategy.cupom;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.MoneyRounder;
import java.math.BigDecimal;

public class Menos50Strategy implements CupomStrategy {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, CheckoutRequest request) {
        return MoneyRounder.round(50.0);
    }

    @Override
    public void validar(BigDecimal subtotalProdutos) {
        if (subtotalProdutos.compareTo(BigDecimal.valueOf(300.0)) < 0) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
    }
}
