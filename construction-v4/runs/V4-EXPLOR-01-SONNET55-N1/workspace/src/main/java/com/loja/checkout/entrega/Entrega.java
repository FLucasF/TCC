package com.loja.checkout.entrega;

import com.loja.checkout.Carrinho;
import com.loja.checkout.Codificado;
import java.math.BigDecimal;

public interface Entrega extends Codificado {
    boolean atende(Carrinho carrinho);

    BigDecimal frete(Carrinho carrinho);

    int prazoDias();
}
