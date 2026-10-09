package com.loja.checkout.calculo.cupom;

import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;

public class Menos50 implements CupomCalculador {
    private static final BigDecimal MINIMO = BigDecimal.valueOf(300.0);
    private static final BigDecimal DESCONTO = BigDecimal.valueOf(50.0);

    @Override
    public boolean ehAplicavel(BigDecimal subtotal) {
        return subtotal.compareTo(MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcular(BigDecimal subtotal, BigDecimal frete) {
        return Arredondador.arredondar(DESCONTO);
    }
}
