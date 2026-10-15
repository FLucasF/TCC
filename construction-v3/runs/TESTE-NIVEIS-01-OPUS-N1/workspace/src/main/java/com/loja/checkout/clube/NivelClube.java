package com.loja.checkout.clube;

import com.loja.checkout.dominio.Codificavel;

import java.math.BigDecimal;

/**
 * Um nivel do clube da loja, com o conjunto de vantagens que ele da. Um nivel
 * novo e uma classe nova, e nada mais.
 */
public interface NivelClube extends Codificavel {

    /** Credito para a proxima compra, sobre os produtos, sem desconto e sem frete. */
    BigDecimal credito(BigDecimal subtotalProdutos);

    /** O frete que o cliente deste nivel paga, a partir do frete da modalidade. */
    BigDecimal frete(BigDecimal freteDaModalidade);

    boolean brinde(BigDecimal subtotalProdutos);
}
