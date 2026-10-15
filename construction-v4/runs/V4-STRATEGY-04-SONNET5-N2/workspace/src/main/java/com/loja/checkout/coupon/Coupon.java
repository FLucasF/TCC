package com.loja.checkout.coupon;

import java.math.BigDecimal;

public interface Coupon {

    String getCodigo();

    boolean isAplicavel(CouponContext contexto);

    BigDecimal calcularDesconto(CouponContext contexto);
}
