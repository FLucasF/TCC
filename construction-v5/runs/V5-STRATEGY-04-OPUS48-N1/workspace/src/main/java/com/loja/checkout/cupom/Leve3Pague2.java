package com.loja.checkout.cupom;

import com.loja.checkout.core.ItemValidado;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(CupomContexto ctx) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemValidado item : ctx.itens()) {
            int gratis = item.quantidade() / 3;
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
        }
        return total;
    }
}
