package com.loja.checkout.calculo.frete;

import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;

public class FreteExpresa implements FreteCalculador {
    @Override
    public BigDecimal calcular(Double pesoTotal) {
        BigDecimal base = BigDecimal.valueOf(25.0);
        BigDecimal porKg = BigDecimal.valueOf(4.5).multiply(BigDecimal.valueOf(pesoTotal));
        return Arredondador.arredondar(base.add(porKg));
    }

    @Override
    public int getPrazo() {
        return 2;
    }
}
