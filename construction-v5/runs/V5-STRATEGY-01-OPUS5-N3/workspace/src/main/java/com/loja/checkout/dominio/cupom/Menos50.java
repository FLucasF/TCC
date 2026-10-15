package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

public final class Menos50 implements Cupom {

    private static final BigDecimal DESCONTO = new BigDecimal("50.00");
    private static final BigDecimal MINIMO_PRODUTOS = new BigDecimal("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
        return DESCONTO;
    }

    @Override
    public boolean aplicavelA(Pedido pedido) {
        return pedido.subtotalProdutos().compareTo(MINIMO_PRODUTOS) >= 0;
    }
}
