package com.loja.checkout.service.clube;

import java.math.BigDecimal;

public interface BeneficioClube {

    String nivel();

    BigDecimal creditoProximaCompra(BigDecimal subtotal);

    boolean freteGratis();

    boolean brinde(BigDecimal subtotal);
}
