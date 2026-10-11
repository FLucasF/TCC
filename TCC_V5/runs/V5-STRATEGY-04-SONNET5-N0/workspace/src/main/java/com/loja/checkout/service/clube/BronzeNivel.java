package com.loja.checkout.service.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class BronzeNivel implements NivelClube {

    @Override
    public String getCodigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return new BigDecimal("0.00");
    }

    @Override
    public boolean isFreteGratis() {
        return false;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
