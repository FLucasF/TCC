package com.loja.checkout.coupon;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FreteGratisCoupon implements Coupon {

    @Override
    public String getCodigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean isAplicavel(CouponContext contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(CouponContext contexto) {
        return contexto.frete();
    }
}
