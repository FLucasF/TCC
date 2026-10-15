package br.com.loja.checkout.entrega;

import br.com.loja.checkout.Carrinho;
import java.math.BigDecimal;

/** Uma opção de entrega. Cada transportadora nova é uma nova implementação. */
public interface ModalidadeEntrega {

    String codigo();

    /** Se a opção pode levar este pedido. */
    default boolean atende(Carrinho carrinho) {
        return true;
    }

    BigDecimal frete(Carrinho carrinho);

    int prazoDias();
}
