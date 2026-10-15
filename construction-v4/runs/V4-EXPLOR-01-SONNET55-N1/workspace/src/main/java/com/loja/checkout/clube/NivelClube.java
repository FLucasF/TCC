package com.loja.checkout.clube;

import com.loja.checkout.Codificado;
import java.math.BigDecimal;

public interface NivelClube extends Codificado {
    BigDecimal credito(BigDecimal subtotal);

    BigDecimal frete(BigDecimal freteDaEntrega);

    boolean brinde(BigDecimal subtotal);
}
