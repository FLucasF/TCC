package com.loja.checkout.entrega;

import com.loja.checkout.domain.Pedido;

import java.math.BigDecimal;

public class RetiradaLoja implements CalculadoraEntrega {
    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return BigDecimal.ZERO;
    }

    @Override
    public int getPrazoEntregaDias() {
        return 1;
    }

    @Override
    public boolean podeAtender(Pedido pedido) {
        return true;
    }
}
