package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(CupomContexto contexto) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (Item item : contexto.itens()) {
            int gratis = item.quantidade() / 3;
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
        }
        return desconto;
    }
}
