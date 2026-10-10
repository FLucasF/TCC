package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class Leve3Pague2 implements Cupom {

    @Override
    public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int unidadesGratis = item.quantidade() / 3;
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
        }
        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }
}
