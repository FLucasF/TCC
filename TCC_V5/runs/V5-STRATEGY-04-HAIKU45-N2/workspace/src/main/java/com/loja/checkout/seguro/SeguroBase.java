package com.loja.checkout.seguro;

import java.math.BigDecimal;
import com.loja.checkout.util.Arredonda;

public abstract class SeguroBase implements CalculadoraSeguro {
    protected abstract BigDecimal obterPercentual();

    @Override
    public BigDecimal calcular(BigDecimal subtotalProdutos) {
        return Arredonda.arredonda(subtotalProdutos.multiply(obterPercentual()));
    }
}
