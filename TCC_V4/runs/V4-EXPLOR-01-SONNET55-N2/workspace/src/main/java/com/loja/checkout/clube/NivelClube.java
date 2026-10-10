package com.loja.checkout.clube;

import com.loja.checkout.Identificavel;
import java.math.BigDecimal;

public interface NivelClube extends Identificavel {
    BigDecimal frete(BigDecimal freteBase);

    BigDecimal credito(BigDecimal subtotal);

    boolean brinde(BigDecimal subtotal);
}
