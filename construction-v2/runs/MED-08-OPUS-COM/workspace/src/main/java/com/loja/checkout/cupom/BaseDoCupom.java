package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** O que um cupom pode olhar para decidir o desconto. */
public record BaseDoCupom(Pedido pedido, BigDecimal frete) {

    public BigDecimal subtotalProdutos() {
        return pedido.subtotalProdutos();
    }
}
