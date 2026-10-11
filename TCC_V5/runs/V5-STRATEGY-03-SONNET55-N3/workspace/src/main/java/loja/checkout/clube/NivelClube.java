package loja.checkout.clube;

import java.math.BigDecimal;
import loja.checkout.Codificado;

public interface NivelClube extends Codificado {
    BigDecimal credito(BigDecimal subtotalProdutos);

    BigDecimal freteCobrado(BigDecimal frete);

    boolean brinde(BigDecimal subtotalProdutos);
}
