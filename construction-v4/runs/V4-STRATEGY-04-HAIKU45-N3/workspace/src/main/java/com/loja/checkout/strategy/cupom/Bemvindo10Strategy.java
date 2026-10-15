package com.loja.checkout.strategy.cupom;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.service.MoneyRounder;
import java.math.BigDecimal;

public class Bemvindo10Strategy implements CupomStrategy {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, CheckoutRequest request) {
        return MoneyRounder.round(subtotalProdutos.multiply(BigDecimal.valueOf(0.10)));
    }

    @Override
    public void validar(BigDecimal subtotalProdutos) {
    }
}
