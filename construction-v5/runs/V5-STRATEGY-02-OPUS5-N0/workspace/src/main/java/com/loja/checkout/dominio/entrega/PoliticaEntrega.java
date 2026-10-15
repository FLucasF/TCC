package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.BaseCalculo;
import com.loja.checkout.dominio.Identificavel;

import java.math.BigDecimal;

/**
 * Uma opcao de entrega. Cada transportadora nova e uma implementacao nova:
 * basta anotar com @Component que ela passa a ser aceita em modalidadeEntrega.
 */
public interface PoliticaEntrega extends Identificavel {

    /** Frete da modalidade, arredondado para centavos. */
    BigDecimal frete(BaseCalculo base);

    /** Prazo de entrega em dias. */
    int prazoEntregaDias();

    /** Se a modalidade atende este pedido (peso, valor, etc.). */
    default boolean atende(BaseCalculo base) {
        return true;
    }
}
