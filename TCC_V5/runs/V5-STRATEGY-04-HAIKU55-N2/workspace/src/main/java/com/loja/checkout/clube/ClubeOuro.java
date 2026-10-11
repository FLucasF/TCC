package com.loja.checkout.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class ClubeOuro implements NivelClube {

    private static final BigDecimal VALOR_MINIMO_BRINDE = new BigDecimal("500");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal percentualCredito() {
        return new BigDecimal("0.05");
    }

    @Override
    public BigDecimal aplicarFrete(BigDecimal frete) {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotal) {
        return subtotal.compareTo(VALOR_MINIMO_BRINDE) > 0;
    }
}
