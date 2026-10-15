package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Moeda;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** 2% dos produtos de volta em credito. */
@Component
public class ClubePrata implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Moeda.percentual(subtotalProdutos, PERCENTUAL_CREDITO);
    }
}
