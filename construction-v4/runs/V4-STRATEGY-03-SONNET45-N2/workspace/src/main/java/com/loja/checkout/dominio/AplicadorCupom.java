package com.loja.checkout.dominio;

import java.math.BigDecimal;

public interface AplicadorCupom {
    boolean podeAplicar(BigDecimal subtotalProdutos);
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete);
}
