package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Carrinho;
import java.math.BigDecimal;

/** Um cupom de desconto. Cada promoção nova é uma nova implementação. */
public interface Cupom {

    String codigo();

    /** Se o pedido cumpre a condição do cupom. */
    default boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    /** Desconto do cupom, conhecendo o carrinho e o frete que aparece no resumo. */
    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
