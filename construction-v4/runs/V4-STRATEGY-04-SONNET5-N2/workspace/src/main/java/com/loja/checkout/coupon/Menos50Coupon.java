package com.loja.checkout.coupon;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Menos50Coupon implements Coupon {

    private static final BigDecimal SUBTOTAL_MINIMO = new BigDecimal("300.00");
    private static final BigDecimal DESCONTO_FIXO = new BigDecimal("50.00");

    @Override
    public String getCodigo() {
        return "MENOS50";
    }

    @Override
    public boolean isAplicavel(CouponContext contexto) {
        return contexto.subtotalProdutos().compareTo(SUBTOTAL_MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(CouponContext contexto) {
        return DESCONTO_FIXO;
    }
}
