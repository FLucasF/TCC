package com.loja.checkout.clube;

import com.loja.checkout.Identificavel;
import java.math.BigDecimal;

/** Um nível do clube e o conjunto de vantagens que ele dá. */
public interface Nivel extends Identificavel {
    BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    boolean isentaFrete();

    boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
