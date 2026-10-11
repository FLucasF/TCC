package br.loja.checkout.clube;

import br.loja.checkout.Identificavel;
import java.math.BigDecimal;

public interface NivelClube extends Identificavel {
    BigDecimal credito(BigDecimal subtotalProdutos);

    BigDecimal frete(BigDecimal frete);

    boolean brinde(BigDecimal subtotalProdutos);
}
