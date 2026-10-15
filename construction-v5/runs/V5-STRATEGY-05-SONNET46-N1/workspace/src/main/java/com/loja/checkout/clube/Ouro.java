package com.loja.checkout.clube;

import com.loja.checkout.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Ouro implements NivelClube {
    private static final BigDecimal TAXA_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() { return "OURO"; }

    @Override
    public boolean temFreteGratis() { return true; }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Arredondamento.arredondar(subtotalProdutos.multiply(TAXA_CREDITO));
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
