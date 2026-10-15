package com.loja.checkout.dominio.clube;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;

/**
 * Nivel do cliente no clube da loja. Para criar um nivel novo basta implementar
 * esta interface e anotar a classe com {@code @Component}.
 */
public interface NivelClube {

    /** Codigo enviado pelo site, ex.: OURO. */
    String codigo();

    /** Percentual dos produtos devolvido em credito para a proxima compra. */
    default BigDecimal percentualCredito() {
        return BigDecimal.ZERO;
    }

    /** Se o nivel isenta o cliente do frete. */
    default boolean freteGratis() {
        return false;
    }

    /** Se o pedido ganha brinde. */
    default boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }

    /** Credito gerado, sobre o valor dos produtos, sem desconto e sem frete. */
    default BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, percentualCredito());
    }
}
