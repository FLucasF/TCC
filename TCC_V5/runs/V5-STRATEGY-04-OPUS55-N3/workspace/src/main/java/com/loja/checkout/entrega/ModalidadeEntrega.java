package com.loja.checkout.entrega;

import com.loja.checkout.Carrinho;
import com.loja.checkout.Codificado;
import java.math.BigDecimal;

public interface ModalidadeEntrega extends Codificado {

    /** Se a modalidade consegue levar este pedido. */
    default boolean atende(Carrinho carrinho) {
        return true;
    }

    /** Frete já arredondado para centavos. */
    BigDecimal frete(Carrinho carrinho);

    int prazoDias();
}
