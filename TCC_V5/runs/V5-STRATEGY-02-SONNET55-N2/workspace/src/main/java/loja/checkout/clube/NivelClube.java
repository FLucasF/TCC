package loja.checkout.clube;

import java.math.BigDecimal;
import loja.checkout.Codificado;

public interface NivelClube extends Codificado {
    BigDecimal frete(BigDecimal freteCalculado);

    BigDecimal credito(BigDecimal subtotalProdutos);

    boolean brinde(BigDecimal subtotalProdutos);
}
