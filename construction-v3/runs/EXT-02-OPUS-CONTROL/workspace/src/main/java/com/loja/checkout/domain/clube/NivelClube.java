package com.loja.checkout.domain.clube;

import com.loja.checkout.domain.pedido.Pedido;
import java.math.BigDecimal;

/**
 * Um nivel do clube da loja. Niveis novos entram como implementacoes
 * anotadas com @Component, cada um com seu conjunto de vantagens.
 */
public interface NivelClube {

    /** Codigo usado na chamada do site, ex.: "OURO". */
    String codigo();

    /** Credito guardado para a proxima compra, arredondado para centavos. */
    BigDecimal creditoProximaCompra(Pedido pedido);

    /** Quando verdadeiro, o frete sai zerado no resumo. */
    default boolean freteGratis() {
        return false;
    }

    /** Quando verdadeiro, a loja manda um brinde junto. */
    default boolean temBrinde(Pedido pedido) {
        return false;
    }
}
