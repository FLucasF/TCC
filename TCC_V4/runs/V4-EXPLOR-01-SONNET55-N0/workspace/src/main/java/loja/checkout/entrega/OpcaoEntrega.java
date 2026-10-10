package loja.checkout.entrega;

import java.math.BigDecimal;
import loja.checkout.dominio.Pedido;

public interface OpcaoEntrega {
    String codigo();

    int prazoDias();

    BigDecimal frete(Pedido pedido);

    default boolean atende(Pedido pedido) {
        return true;
    }
}
