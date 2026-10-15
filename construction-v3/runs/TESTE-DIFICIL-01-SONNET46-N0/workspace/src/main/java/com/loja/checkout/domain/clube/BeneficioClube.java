package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

public interface BeneficioClube {

    String getCodigo();

    boolean isFreteGratis();

    boolean isJurosSemAte6x();

    BigDecimal calcularCredito(BigDecimal subtotal);

    boolean temBrinde(BigDecimal subtotal);
}
