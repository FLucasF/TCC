package com.loja.checkout.clube;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/**
 * Um nivel do clube da loja. Para criar um nivel novo, basta uma classe nova que
 * implemente esta interface e marcar com @Component: ela entra sozinha no {@link NiveisClube}.
 */
public interface NivelClube {

    /** Codigo que o site manda no campo nivelClube. */
    String codigo();

    /** Credito para a proxima compra, sobre o valor dos produtos (sem desconto e sem frete). */
    default BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    /** Se o nivel isenta o frete. */
    default boolean freteGratis() {
        return false;
    }

    /** Se o pedido vai com brinde. */
    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
