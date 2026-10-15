package com.loja.checkout.cupom;

import com.loja.checkout.ItemCarrinho;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(ContextoCupom ctx) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom ctx) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemCarrinho item : ctx.itens()) {
            int gratuitos = item.quantidade() / 3;
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratuitos)));
        }
        return desconto.setScale(2, RoundingMode.HALF_EVEN);
    }
}
