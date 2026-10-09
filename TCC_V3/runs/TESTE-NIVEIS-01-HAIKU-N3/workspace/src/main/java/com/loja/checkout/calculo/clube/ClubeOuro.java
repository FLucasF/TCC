package com.loja.checkout.calculo.clube;

import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;

public class ClubeOuro implements ClubeCalculador {
    private static final BigDecimal MINIMO_BRINDE = BigDecimal.valueOf(500.0);

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotal) {
        return Arredondador.arredondar(subtotal.multiply(BigDecimal.valueOf(0.05)));
    }

    @Override
    public boolean temFreteGratis() {
        return true;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotal) {
        return subtotal.compareTo(MINIMO_BRINDE) > 0;
    }
}
