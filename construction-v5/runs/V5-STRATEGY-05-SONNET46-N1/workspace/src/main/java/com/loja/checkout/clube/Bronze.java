package com.loja.checkout.clube;

import com.loja.checkout.Arredondamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Bronze implements NivelClube {
    @Override
    public String codigo() { return "BRONZE"; }

    @Override
    public boolean temFreteGratis() { return false; }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Arredondamento.ZERO;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) { return false; }
}
