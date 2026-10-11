package com.loja.checkout.entrega;

import com.loja.checkout.Pedido;

import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {
    @Override
    public void validar(Pedido pedido) {}

    @Override
    public BigDecimal frete(Pedido pedido) {
        return BigDecimal.ZERO;
    }

    @Override
    public int prazoDias() { return 1; }
}
