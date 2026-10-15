package br.com.loja.checkout.entrega;

import br.com.loja.checkout.Identificavel;
import br.com.loja.checkout.Pedido;
import java.math.BigDecimal;

public interface Entrega extends Identificavel {

    default boolean atende(Pedido pedido) {
        return true;
    }

    BigDecimal frete(Pedido pedido);

    int prazoDias();
}
