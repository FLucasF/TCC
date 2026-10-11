package com.loja.checkout.seguro;

import java.math.BigDecimal;

public interface CalculadoraSeguro {
    BigDecimal calcular(BigDecimal subtotalProdutos);
}
