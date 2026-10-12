package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Prata implements NivelClube {

    private static final BigDecimal ALIQUOTA_CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, ALIQUOTA_CREDITO);
    }
}
