package com.loja.service.estrategia;

import com.loja.util.Arredondador;
import java.math.BigDecimal;

public class CupomBemvindo10 implements CalculoCupom {
    private static final BigDecimal PERCENTUAL = new BigDecimal("0.10");

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete) {
        BigDecimal desconto = subtotalProdutos.multiply(PERCENTUAL);
        return Arredondador.arredondarParaCentavos(desconto);
    }

    @Override
    public BigDecimal calcularDescontoFrete(BigDecimal frete) {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
