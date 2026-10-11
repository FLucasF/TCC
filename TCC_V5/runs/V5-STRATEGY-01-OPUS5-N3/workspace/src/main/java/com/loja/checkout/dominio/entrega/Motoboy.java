package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

public final class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal PRECO = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return PRECO;
    }

    @Override
    public int prazoDias() {
        return 0;
    }

    @Override
    public boolean atende(Pedido pedido) {
        return pedido.pesoKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }
}
