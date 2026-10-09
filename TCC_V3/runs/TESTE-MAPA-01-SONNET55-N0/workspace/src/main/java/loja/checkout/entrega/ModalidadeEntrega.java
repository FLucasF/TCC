package loja.checkout.entrega;

import java.math.BigDecimal;
import loja.checkout.dominio.Pedido;

public interface ModalidadeEntrega {
    String codigo();

    int prazoDias();

    default boolean atende(Pedido pedido) {
        return true;
    }

    /** Frete já arredondado para centavos. */
    BigDecimal frete(Pedido pedido);
}
