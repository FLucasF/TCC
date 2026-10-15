package br.loja.checkout.entrega;

import br.loja.checkout.Identificavel;
import br.loja.checkout.Pedido;
import java.math.BigDecimal;

public interface Entrega extends Identificavel {
    boolean atende(Pedido pedido);

    BigDecimal frete(Pedido pedido);

    int prazoDias(Pedido pedido);
}
