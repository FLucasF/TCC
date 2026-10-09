package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Ganha 2% dos produtos de volta em credito. */
@Component
public class Prata implements NivelClube {

    private static final BigDecimal TAXA_CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, TAXA_CREDITO);
    }
}
