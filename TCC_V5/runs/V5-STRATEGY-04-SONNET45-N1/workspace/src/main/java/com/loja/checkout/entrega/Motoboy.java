package com.loja.checkout.entrega;

import com.loja.checkout.domain.Pedido;

import java.math.BigDecimal;

public class Motoboy implements CalculadoraEntrega {
    private static final BigDecimal TAXA_FIXA = new BigDecimal("18.00");
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5.0");

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return TAXA_FIXA;
    }

    @Override
    public int getPrazoEntregaDias() {
        return 0;
    }

    @Override
    public boolean podeAtender(Pedido pedido) {
        return pedido.getPesoTotal().compareTo(PESO_MAXIMO) <= 0;
    }
}
