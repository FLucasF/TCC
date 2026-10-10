package com.loja.checkout.clube;

import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.entrega.ModalidadeEntrega;

import java.math.BigDecimal;

/** Um nivel do clube da loja, com seu proprio conjunto de vantagens. */
public interface NivelClube {

    String codigo();

    /** Credito para a proxima compra, sobre os produtos, em centavos. */
    BigDecimal credito(BigDecimal subtotalProdutos);

    /** O frete que este nivel paga pela entrega escolhida. */
    BigDecimal frete(ModalidadeEntrega entrega, Pedido pedido);

    /** Se a loja manda um brinde junto. */
    boolean brinde(BigDecimal subtotalProdutos);
}
