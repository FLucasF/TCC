package com.loja.clube;

import com.loja.util.Arredondador;
import java.math.BigDecimal;

public class Ouro implements NivelClube {
    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal VALOR_MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        BigDecimal credito = subtotalProdutos.multiply(PERCENTUAL_CREDITO);
        return Arredondador.arredondar(credito);
    }

    @Override
    public boolean ganhaFreteGratis() {
        return true;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(VALOR_MINIMO_BRINDE) > 0;
    }
}
