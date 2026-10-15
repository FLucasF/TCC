package com.loja.checkout.dominio.frete;

import com.loja.checkout.dominio.CalculadoraFrete;
import java.math.BigDecimal;

public class FreteMotoboy implements CalculadoraFrete {

    @Override
    public BigDecimal calcular(BigDecimal pesoTotal) {
        return new BigDecimal("18.00");
    }

    @Override
    public int prazoEmDias() {
        return 0;
    }

    @Override
    public boolean aceitaPedido(BigDecimal pesoTotal) {
        return pesoTotal.compareTo(new BigDecimal("5")) <= 0;
    }
}
