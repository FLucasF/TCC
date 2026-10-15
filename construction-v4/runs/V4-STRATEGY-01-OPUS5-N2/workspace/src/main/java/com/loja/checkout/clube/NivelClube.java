package com.loja.checkout.clube;

import com.loja.checkout.dominio.Identificavel;
import java.math.BigDecimal;

/**
 * Um nivel do clube da loja. Cada nivel novo e' uma implementacao desta
 * interface: traz seu conjunto de vantagens.
 */
public interface NivelClube extends Identificavel {

    /** O credito que fica guardado para a proxima compra. */
    BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    /** Quanto do frete da modalidade o cliente paga. */
    BigDecimal freteDevido(BigDecimal freteDaModalidade);

    /** Se vai brinde junto com o pedido. */
    boolean brinde(BigDecimal subtotalProdutos);
}
