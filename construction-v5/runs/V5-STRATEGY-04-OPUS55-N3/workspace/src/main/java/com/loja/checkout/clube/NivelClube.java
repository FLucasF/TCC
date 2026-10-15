package com.loja.checkout.clube;

import com.loja.checkout.Codificado;
import java.math.BigDecimal;

public interface NivelClube extends Codificado {

    /** Frete que o cliente paga, dado o frete da modalidade. */
    default BigDecimal frete(BigDecimal freteModalidade) {
        return freteModalidade;
    }

    /** Crédito para a próxima compra, já arredondado para centavos. */
    BigDecimal credito(BigDecimal subtotalProdutos);

    boolean brinde(BigDecimal subtotalProdutos);
}
