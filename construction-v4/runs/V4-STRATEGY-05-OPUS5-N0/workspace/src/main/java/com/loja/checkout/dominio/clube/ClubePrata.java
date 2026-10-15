package com.loja.checkout.dominio.clube;

import com.loja.checkout.comum.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Ganha 2% dos produtos de volta em credito. */
@Component
public class ClubePrata implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("2");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, PERCENTUAL_CREDITO);
    }
}
