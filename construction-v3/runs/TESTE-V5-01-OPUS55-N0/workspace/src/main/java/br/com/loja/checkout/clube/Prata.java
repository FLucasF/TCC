package br.com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 2% dos produtos de volta em crédito. */
@Component
public class Prata implements NivelClube {

    private static final BigDecimal TAXA_CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal taxaCredito() {
        return TAXA_CREDITO;
    }
}
