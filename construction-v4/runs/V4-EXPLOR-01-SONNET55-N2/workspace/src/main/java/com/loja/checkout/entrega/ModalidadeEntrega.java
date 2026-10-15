package com.loja.checkout.entrega;

import com.loja.checkout.Carrinho;
import com.loja.checkout.Identificavel;
import java.math.BigDecimal;

public interface ModalidadeEntrega extends Identificavel {
    int prazoDias();

    boolean atende(Carrinho carrinho);

    BigDecimal frete(Carrinho carrinho);
}
