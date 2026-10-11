package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Codificavel;
import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** Uma forma de o cliente receber o pedido: quanto cobra, em quanto tempo e o que aceita levar. */
public interface ModalidadeEntrega extends Codificavel {

    BigDecimal frete(Pedido pedido);

    int prazoDias();

    default boolean atende(Pedido pedido) {
        return true;
    }
}
