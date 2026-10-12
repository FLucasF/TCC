package loja.checkout.clube;

import loja.checkout.Codificado;
import loja.checkout.Dinheiro;
import loja.checkout.Pedido;

import java.math.BigDecimal;

/** Cada nível mora na sua própria classe; o padrão é não ganhar nada. */
public interface NivelClube extends Codificado {

    default BigDecimal frete(BigDecimal freteDaEntrega) {
        return freteDaEntrega;
    }

    default BigDecimal credito(Pedido pedido) {
        return Dinheiro.ZERO;
    }

    default boolean brinde(Pedido pedido) {
        return false;
    }
}
