package com.loja.checkout.calculo.cupom;

import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;

public class BemVindo10 implements CupomCalculador {
    @Override
    public boolean ehAplicavel(BigDecimal subtotal) {
        return true;
    }

    @Override
    public BigDecimal calcular(BigDecimal subtotal, BigDecimal frete) {
        return Arredondador.arredondar(subtotal.multiply(BigDecimal.valueOf(0.1)));
    }
}
