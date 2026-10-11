package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Ganha 2% do valor dos produtos de volta, em crédito para a próxima compra. */
@Component
public class NivelPrata implements NivelClube {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.centavos(subtotalProdutos.multiply(PERCENTUAL));
    }
}
