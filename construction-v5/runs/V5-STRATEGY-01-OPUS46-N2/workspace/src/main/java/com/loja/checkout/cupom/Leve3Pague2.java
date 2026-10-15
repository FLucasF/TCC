package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.loja.checkout.dto.ItemRequest;
import org.springframework.stereotype.Component;

@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String getCodigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean isAplicavel(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int gratis = item.quantidade() / 3;
            desconto = desconto.add(item.precoUnitario().multiply(new BigDecimal(gratis)));
        }
        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }
}
