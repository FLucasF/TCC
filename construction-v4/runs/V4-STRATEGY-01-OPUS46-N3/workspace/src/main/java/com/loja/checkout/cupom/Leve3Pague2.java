package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotal) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int gratis = item.quantidade() / 3;
            desconto = desconto.add(item.precoUnitario().multiply(new BigDecimal(gratis)));
        }
        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }
}
