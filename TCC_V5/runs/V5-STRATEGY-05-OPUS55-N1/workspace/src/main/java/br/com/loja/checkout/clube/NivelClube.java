package br.com.loja.checkout.clube;

import br.com.loja.checkout.Codificado;

import java.math.BigDecimal;

/** Um nível do clube com suas vantagens. Para incluir um novo, basta criar outro componente que implemente esta interface. */
public interface NivelClube extends Codificado {

    default BigDecimal frete(BigDecimal freteDaEntrega) {
        return freteDaEntrega;
    }

    BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
