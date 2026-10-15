package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Identificavel;
import java.math.BigDecimal;

public interface Entrega extends Identificavel {

    int prazoDias();

    boolean atende(Carrinho carrinho);

    BigDecimal frete(Carrinho carrinho);
}
