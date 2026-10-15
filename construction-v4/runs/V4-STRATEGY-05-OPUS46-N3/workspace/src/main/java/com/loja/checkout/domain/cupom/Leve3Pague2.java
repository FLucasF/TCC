package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Moeda;
import com.loja.checkout.dto.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int gratis = item.quantidade() / 3;
            if (gratis > 0) {
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
        }
        return Moeda.arredondar(desconto);
    }
}
