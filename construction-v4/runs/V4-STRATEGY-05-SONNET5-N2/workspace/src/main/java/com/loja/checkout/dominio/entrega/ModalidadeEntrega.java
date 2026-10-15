package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Carrinho;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    BigDecimal custo(Carrinho carrinho);

    int prazoDias();

    default boolean disponivel(Carrinho carrinho) {
        return true;
    }
}
