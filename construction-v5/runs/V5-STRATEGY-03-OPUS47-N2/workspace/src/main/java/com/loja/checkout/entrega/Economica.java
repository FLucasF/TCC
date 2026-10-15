package com.loja.checkout.entrega;

import com.loja.checkout.Pedido;

import java.math.BigDecimal;

public class Economica implements ModalidadeEntrega {
    private static final BigDecimal BASE = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public void validar(Pedido pedido) {}

    @Override
    public BigDecimal frete(Pedido pedido) {
        return BASE.add(POR_KG.multiply(pedido.pesoKg()));
    }

    @Override
    public int prazoDias() { return 7; }
}
