package com.loja.checkout.calculo.frete;

import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;

public class FreteMotoboy implements FreteCalculador {
    @Override
    public BigDecimal calcular(Double pesoTotal) {
        return Arredondador.arredondar(BigDecimal.valueOf(18.0));
    }

    @Override
    public int getPrazo() {
        return 0;
    }
}
