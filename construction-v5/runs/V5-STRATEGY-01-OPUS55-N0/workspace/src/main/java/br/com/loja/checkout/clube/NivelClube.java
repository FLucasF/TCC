package br.com.loja.checkout.clube;

import java.math.BigDecimal;

/**
 * Um nível do clube e suas vantagens. Por padrão o nível não ganha nada;
 * cada nível sobrescreve só as vantagens que oferece. Para criar um novo,
 * basta implementar esta interface em uma classe anotada com {@code @Component}.
 */
public interface NivelClube {

    String codigo();

    /** Porcentagem dos produtos devolvida em crédito para a próxima compra. */
    default BigDecimal percentualCredito() {
        return BigDecimal.ZERO;
    }

    default boolean isentaFrete() {
        return false;
    }

    default boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
