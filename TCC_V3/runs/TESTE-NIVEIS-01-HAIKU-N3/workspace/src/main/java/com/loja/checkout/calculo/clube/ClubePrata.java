package com.loja.checkout.calculo.clube;

import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;

public class ClubePrata implements ClubeCalculador {
    @Override
    public BigDecimal calcularCredito(BigDecimal subtotal) {
        return Arredondador.arredondar(subtotal.multiply(BigDecimal.valueOf(0.02)));
    }

    @Override
    public boolean temFreteGratis() {
        return false;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotal) {
        return false;
    }
}
