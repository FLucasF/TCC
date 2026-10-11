package loja.checkout.clube;

import java.math.BigDecimal;
import loja.checkout.dominio.Identificavel;

public interface NivelClube extends Identificavel {

    /** Frete cobrado deste nível, dado o frete da entrega escolhida. */
    BigDecimal frete(BigDecimal freteDaEntrega);

    BigDecimal credito(BigDecimal subtotalProdutos);

    boolean brinde(BigDecimal subtotalProdutos);
}
