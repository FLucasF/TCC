package com.loja.checkout.calculo.frete;

import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;

public class FreteRetiradaLoja implements FreteCalculador {
    @Override
    public BigDecimal calcular(Double pesoTotal) {
        return Arredondador.arredondar(BigDecimal.ZERO);
    }

    @Override
    public int getPrazo() {
        return 1;
    }
}
