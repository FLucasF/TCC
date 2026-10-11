package com.loja.strategy.cupom;

import com.loja.util.Arredondamento;
import java.math.BigDecimal;

public class BemVindo10 implements Cupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete) {
        return Arredondamento.arredondar(
            subtotalProdutos.multiply(new BigDecimal("0.10"))
        );
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
