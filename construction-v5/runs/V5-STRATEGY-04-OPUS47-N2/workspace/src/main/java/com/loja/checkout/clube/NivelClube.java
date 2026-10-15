package com.loja.checkout.clube;

import java.math.BigDecimal;

public interface NivelClube {
    BigDecimal frete(BigDecimal freteBase);
    BigDecimal creditoProximaCompra(BigDecimal subtotal);
    boolean brinde(BigDecimal subtotal);
}
