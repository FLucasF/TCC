package com.loja.checkout.dominio.frete;

import com.loja.checkout.dominio.CalculadoraFrete;
import java.math.BigDecimal;

public class FreteRetiradaLoja implements CalculadoraFrete {

    @Override
    public BigDecimal calcular(BigDecimal pesoTotal) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int prazoEmDias() {
        return 1;
    }

    @Override
    public boolean aceitaPedido(BigDecimal pesoTotal) {
        return true;
    }
}
