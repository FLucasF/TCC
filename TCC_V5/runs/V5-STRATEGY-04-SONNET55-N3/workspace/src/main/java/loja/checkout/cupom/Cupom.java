package loja.checkout.cupom;

import java.math.BigDecimal;

import loja.checkout.Carrinho;

public interface Cupom {

    String codigo();

    /** Se o pedido cumpre a condição do cupom. */
    default boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    /** Desconto do cupom; recebe o frete já definido, pois há cupom que o abate. */
    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
