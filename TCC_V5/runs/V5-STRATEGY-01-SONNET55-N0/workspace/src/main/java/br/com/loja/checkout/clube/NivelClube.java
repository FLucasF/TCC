package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/** Para um novo nivel, basta criar um @Component que implemente esta interface. */
public interface NivelClube {

    String codigo();

    /** Fracao do valor dos produtos devolvida em credito (ex.: 0.02). */
    default BigDecimal taxaCredito() {
        return BigDecimal.ZERO;
    }

    default boolean freteGratis() {
        return false;
    }

    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }

    default BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(taxaCredito()));
    }
}
