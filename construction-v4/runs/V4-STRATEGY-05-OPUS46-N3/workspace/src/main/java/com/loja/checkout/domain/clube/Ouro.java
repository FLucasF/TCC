package com.loja.checkout.domain.clube;

import com.loja.checkout.domain.Moeda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Ouro implements NivelClube {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.05");
    private static final BigDecimal LIMIAR_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Moeda.arredondar(subtotalProdutos.multiply(PERCENTUAL));
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMIAR_BRINDE) > 0;
    }
}
