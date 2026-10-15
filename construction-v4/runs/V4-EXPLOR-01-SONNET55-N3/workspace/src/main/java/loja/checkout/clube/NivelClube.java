package loja.checkout.clube;

import java.math.BigDecimal;

import loja.checkout.comum.Codigo;
import loja.checkout.comum.Compra;
import loja.checkout.comum.Dinheiro;

public interface NivelClube extends Codigo {
    default BigDecimal credito(Compra compra) {
        return Dinheiro.ZERO;
    }

    default BigDecimal frete(BigDecimal frete) {
        return frete;
    }

    default boolean brinde(Compra compra) {
        return false;
    }
}
