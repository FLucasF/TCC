package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** Motoboy: valor fixo, no mesmo dia, e so leva pedidos de ate 5 kg. */
public final class Motoboy implements ModalidadeEntrega {

    private static final BigDecimal VALOR = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO_KG = new BigDecimal("5");

    @Override
    public String codigo() {
        return "MOTOBOY";
    }

    @Override
    public boolean atende(Pedido pedido) {
        return pedido.pesoKg().compareTo(PESO_MAXIMO_KG) <= 0;
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return VALOR;
    }

    @Override
    public int prazoDias() {
        return 0;
    }
}
