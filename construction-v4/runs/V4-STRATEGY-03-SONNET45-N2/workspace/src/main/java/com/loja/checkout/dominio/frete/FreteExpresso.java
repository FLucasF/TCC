package com.loja.checkout.dominio.frete;

import com.loja.checkout.dominio.CalculadoraFrete;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class FreteExpresso implements CalculadoraFrete {

    @Override
    public BigDecimal calcular(BigDecimal pesoTotal) {
        BigDecimal base = new BigDecimal("25.00");
        BigDecimal porKg = new BigDecimal("4.50");
        return base.add(porKg.multiply(pesoTotal))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public int prazoEmDias() {
        return 2;
    }

    @Override
    public boolean aceitaPedido(BigDecimal pesoTotal) {
        return true;
    }
}
