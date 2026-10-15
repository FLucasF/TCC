package loja.checkout.clube;

import java.math.BigDecimal;
import loja.checkout.dominio.Dinheiro;
import loja.checkout.dominio.Pedido;

public interface NivelClube {
    String codigo();

    default BigDecimal credito(Pedido pedido) {
        return Dinheiro.ZERO;
    }

    default boolean freteGratis() {
        return false;
    }

    default boolean brinde(Pedido pedido) {
        return false;
    }
}
