package com.loja.checkout.coupon;

import com.loja.checkout.util.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Bemvindo10Coupon implements Coupon {

    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.10");

    @Override
    public String getCodigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean isAplicavel(CouponContext contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(CouponContext contexto) {
        return Money.round(contexto.subtotalProdutos().multiply(PERCENTUAL_DESCONTO));
    }
}
