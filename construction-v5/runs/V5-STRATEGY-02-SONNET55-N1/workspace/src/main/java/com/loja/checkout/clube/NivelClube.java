package com.loja.checkout.clube;

import com.loja.checkout.dominio.Identificavel;
import java.math.BigDecimal;

public interface NivelClube extends Identificavel {

    BigDecimal credito(BigDecimal subtotalProdutos);

    boolean isentaFrete();

    boolean brinde(BigDecimal subtotalProdutos);
}
