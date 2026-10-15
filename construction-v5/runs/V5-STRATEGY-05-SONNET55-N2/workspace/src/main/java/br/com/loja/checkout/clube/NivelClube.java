package br.com.loja.checkout.clube;

import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.Identificavel;
import java.math.BigDecimal;

public interface NivelClube extends Identificavel {

    default BigDecimal freteCobrado(BigDecimal frete) {
        return frete;
    }

    default BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
