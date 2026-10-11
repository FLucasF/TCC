package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Carrinho;
import java.math.BigDecimal;

/** Um cupom de desconto. Cada promoção nova entra como uma nova implementação. */
public interface Cupom {

    String codigo();

    /** Se o pedido cumpre a condição do cupom. */
    default boolean aplicavel(Carrinho carrinho, BigDecimal frete) {
        return true;
    }

    /** Desconto concedido, já arredondado para centavos. */
    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
