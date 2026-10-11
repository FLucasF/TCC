package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/** Novo nível = nova classe @Component implementando esta interface. */
public interface NivelClube {

    String codigo();

    default BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    default boolean freteGratis() {
        return false;
    }

    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
