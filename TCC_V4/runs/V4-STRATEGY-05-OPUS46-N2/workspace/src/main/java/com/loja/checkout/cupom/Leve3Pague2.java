package com.loja.checkout.cupom;

import com.loja.checkout.CheckoutRequest.ItemRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class Leve3Pague2 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int gratuitos = item.quantidade() / 3;
            if (gratuitos > 0) {
                desconto = desconto.add(
                        item.precoUnitario().multiply(new BigDecimal(gratuitos))
                );
            }
        }
        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens) {
        return true;
    }
}
