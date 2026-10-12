package br.com.loja.checkout.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Prata implements NivelClube {

    private static final BigDecimal CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal percentualCredito() {
        return CREDITO;
    }
}
