package loja.checkout.cupom;

import java.math.BigDecimal;
import loja.checkout.dominio.Pedido;

public interface Cupom {
    String codigo();

    default boolean aplicavel(Pedido pedido) {
        return true;
    }

    /** Desconto já arredondado para centavos. {@code frete} é o frete final do pedido. */
    BigDecimal desconto(Pedido pedido, BigDecimal frete);
}
