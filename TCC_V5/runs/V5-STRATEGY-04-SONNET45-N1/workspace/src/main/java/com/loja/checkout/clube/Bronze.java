package com.loja.checkout.clube;

import com.loja.checkout.domain.Pedido;

import java.math.BigDecimal;

public class Bronze implements BeneficioClube {
    @Override
    public BigDecimal calcularCredito(Pedido pedido) {
        return BigDecimal.ZERO;
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
