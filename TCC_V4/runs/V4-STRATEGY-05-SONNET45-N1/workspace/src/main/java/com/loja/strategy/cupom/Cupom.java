package com.loja.strategy.cupom;

import com.loja.dto.CheckoutRequest;
import com.loja.model.DadosPedido;
import java.math.BigDecimal;

public interface Cupom {
    boolean isAplicavel(BigDecimal subtotalProdutos, CheckoutRequest request);
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, CheckoutRequest request);
}
