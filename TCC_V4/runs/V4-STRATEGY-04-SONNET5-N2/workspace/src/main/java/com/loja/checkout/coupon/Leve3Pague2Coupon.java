package com.loja.checkout.coupon;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.util.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Leve3Pague2Coupon implements Coupon {

    private static final int UNIDADES_POR_BRINDE = 3;

    @Override
    public String getCodigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean isAplicavel(CouponContext contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(CouponContext contexto) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : contexto.itens()) {
            int unidadesGratis = item.quantidade() / UNIDADES_POR_BRINDE;
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
        }
        return Money.round(desconto);
    }
}
