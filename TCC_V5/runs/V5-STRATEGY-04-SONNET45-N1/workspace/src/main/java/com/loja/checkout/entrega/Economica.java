package com.loja.checkout.entrega;

import com.loja.checkout.domain.Pedido;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Economica implements CalculadoraEntrega {
    private static final BigDecimal TAXA_BASE = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        BigDecimal pesoTotal = pedido.getPesoTotal();
        return TAXA_BASE.add(TAXA_POR_KG.multiply(pesoTotal))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int getPrazoEntregaDias() {
        return 7;
    }

    @Override
    public boolean podeAtender(Pedido pedido) {
        return true;
    }
}
