package com.loja.checkout.coupon;

import java.math.BigDecimal;

public interface Coupon {

    String codigo();

    boolean aplicavel(CupomContexto contexto);

    BigDecimal calcularDesconto(CupomContexto contexto);
}
