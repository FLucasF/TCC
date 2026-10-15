package loja.checkout.clube;

import java.math.BigDecimal;

public interface NivelClube {

    String codigo();

    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    BigDecimal credito(BigDecimal subtotalProdutos);

    /** Frete que o cliente deste nível paga, dado o frete da modalidade. */
    default BigDecimal frete(BigDecimal freteModalidade) {
        return freteModalidade;
    }

    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
