package com.loja.checkout.strategy.cupom;

import com.loja.checkout.dto.CheckoutRequest;
import java.math.BigDecimal;

public interface CupomStrategy {
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, CheckoutRequest request);

    void validar(BigDecimal subtotalProdutos);
}
