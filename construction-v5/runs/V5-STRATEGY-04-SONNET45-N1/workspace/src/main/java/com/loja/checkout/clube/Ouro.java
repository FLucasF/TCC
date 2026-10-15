package com.loja.checkout.clube;

import com.loja.checkout.domain.Pedido;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Ouro implements BeneficioClube {
    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal VALOR_MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public BigDecimal calcularCredito(Pedido pedido) {
        return pedido.getSubtotalProdutos()
                .multiply(PERCENTUAL_CREDITO)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean temFreteGratis() {
        return true;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(VALOR_MINIMO_BRINDE) > 0;
    }
}
