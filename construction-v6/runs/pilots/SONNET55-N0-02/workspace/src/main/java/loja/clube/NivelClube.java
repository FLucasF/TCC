package loja.clube;

import java.math.BigDecimal;

/** Um nível do clube. Para criar outro, basta um novo @Component que implemente esta interface. */
public interface NivelClube {

    String codigo();

    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    BigDecimal credito(BigDecimal subtotalProdutos);

    default boolean isentaFrete() {
        return false;
    }

    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
