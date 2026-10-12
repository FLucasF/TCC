package loja.checkout.cupom;

import java.math.BigDecimal;

import loja.checkout.Carrinho;
import loja.checkout.Codificado;

public interface Cupom extends Codificado {

    /** Se o pedido cumpre a condição do cupom. */
    default boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    /** Desconto em reais; o frete é o já calculado para o pedido. */
    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
