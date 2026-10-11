package com.loja.checkout.entrega;

import com.loja.checkout.comum.Carrinho;
import com.loja.checkout.comum.Codificado;
import java.math.BigDecimal;

/** Uma opção de entrega: como cobra, qual o prazo e quais pedidos atende. */
public interface ModalidadeEntrega extends Codificado {

    boolean atende(Carrinho carrinho);

    /** Frete já arredondado para centavos. */
    BigDecimal frete(Carrinho carrinho);

    int prazoDias();
}
