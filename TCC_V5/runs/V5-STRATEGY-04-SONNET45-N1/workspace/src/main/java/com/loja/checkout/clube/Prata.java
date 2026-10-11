package com.loja.checkout.clube;

import com.loja.checkout.domain.Pedido;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Prata implements BeneficioClube {
    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public BigDecimal calcularCredito(Pedido pedido) {
        return pedido.getSubtotalProdutos()
                .multiply(PERCENTUAL_CREDITO)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean temFreteGratis() {
        return false;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
