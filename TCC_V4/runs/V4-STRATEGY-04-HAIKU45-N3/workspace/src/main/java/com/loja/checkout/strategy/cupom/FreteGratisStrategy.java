package com.loja.checkout.strategy.cupom;

import com.loja.checkout.dto.CheckoutRequest;
import java.math.BigDecimal;

public class FreteGratisStrategy implements CupomStrategy {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, CheckoutRequest request) {
        return BigDecimal.ZERO;
    }

    @Override
    public void validar(BigDecimal subtotalProdutos) {
    }
}
