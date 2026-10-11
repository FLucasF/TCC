package loja.checkout.entrega;

import java.math.BigDecimal;

import loja.checkout.Carrinho;

public interface ModalidadeEntrega {

    String codigo();

    int prazoDias();

    /** Se a modalidade consegue atender o pedido. */
    default boolean atende(Carrinho carrinho) {
        return true;
    }

    BigDecimal frete(Carrinho carrinho);
}
