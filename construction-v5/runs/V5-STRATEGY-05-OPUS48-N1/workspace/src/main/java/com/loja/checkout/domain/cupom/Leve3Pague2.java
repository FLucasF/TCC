package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.ItemPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(CupomContexto contexto) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemPedido item : contexto.itens()) {
            int gratis = item.quantidade() / 3;
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
        }
        return desconto;
    }
}
