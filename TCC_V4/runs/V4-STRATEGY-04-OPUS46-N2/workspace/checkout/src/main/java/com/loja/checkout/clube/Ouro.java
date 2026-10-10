package com.loja.checkout.clube;

import com.loja.checkout.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Ouro implements NivelClube {

    private static final BigDecimal CASHBACK = new BigDecimal("0.05");
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Arredondamento.centavos(subtotalProdutos.multiply(CASHBACK));
    }

    @Override
    public boolean isFreteGratis() {
        return true;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(LIMITE_BRINDE) > 0;
    }
}
