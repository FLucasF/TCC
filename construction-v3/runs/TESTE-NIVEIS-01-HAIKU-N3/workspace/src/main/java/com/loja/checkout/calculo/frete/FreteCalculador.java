package com.loja.checkout.calculo.frete;

import java.math.BigDecimal;

public interface FreteCalculador {
    BigDecimal calcular(Double pesoTotal);
    int getPrazo();
}
