package loja.checkout.clube;

import java.math.BigDecimal;
import loja.checkout.dominio.Dinheiro;

public interface NivelClube {
    String codigo();

    default boolean isentoDeFrete() {
        return false;
    }

    default BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
