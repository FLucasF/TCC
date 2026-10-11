package br.com.loja.checkout.clube;

import java.math.BigDecimal;

public interface NivelClube {

    String codigo();

    Vantagens vantagens(BigDecimal subtotalProdutos);
}
