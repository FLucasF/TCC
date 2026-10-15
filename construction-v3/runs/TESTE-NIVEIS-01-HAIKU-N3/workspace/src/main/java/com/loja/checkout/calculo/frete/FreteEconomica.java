package com.loja.checkout.calculo.frete;

import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;

public class FreteEconomica implements FreteCalculador {
    @Override
    public BigDecimal calcular(Double pesoTotal) {
        BigDecimal base = BigDecimal.valueOf(12.0);
        BigDecimal porKg = BigDecimal.valueOf(2.0).multiply(BigDecimal.valueOf(pesoTotal));
        return Arredondador.arredondar(base.add(porKg));
    }

    @Override
    public int getPrazo() {
        return 7;
    }
}
