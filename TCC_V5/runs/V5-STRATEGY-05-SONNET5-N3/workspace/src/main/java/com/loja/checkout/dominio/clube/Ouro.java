package com.loja.checkout.dominio.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Ouro implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal percentualCredito() {
        return PERCENTUAL_CREDITO;
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public boolean elegivelBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
