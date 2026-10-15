package br.com.loja.checkout.clube;

import br.com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Um nível do clube da loja e suas vantagens. Para criar um novo nível, basta escrever
 * uma classe que implemente esta interface e anotá-la com {@code @Component}.
 */
public interface NivelClube {

    /** Código enviado pelo site em {@code nivelClube}. */
    String codigo();

    /** Crédito para a próxima compra, já arredondado para centavos. */
    BigDecimal credito(Pedido pedido);

    /** Se o cliente deste nível fica isento do frete. */
    default boolean isentoDeFrete() {
        return false;
    }

    /** Se o pedido vai com brinde. */
    default boolean ganhaBrinde(Pedido pedido) {
        return false;
    }
}
