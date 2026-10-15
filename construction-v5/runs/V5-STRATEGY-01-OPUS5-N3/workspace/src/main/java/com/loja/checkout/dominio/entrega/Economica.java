package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

public final class Economica implements ModalidadeEntrega {

    private static final BigDecimal BASE = new BigDecimal("12.00");
    private static final BigDecimal POR_KG = new BigDecimal("2.00");

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.centavos(BASE.add(POR_KG.multiply(pedido.pesoKg())));
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
