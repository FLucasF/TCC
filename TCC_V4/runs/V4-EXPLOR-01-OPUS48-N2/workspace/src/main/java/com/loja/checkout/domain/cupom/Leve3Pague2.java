package com.loja.checkout.domain.cupom;

import com.loja.checkout.api.ItemCarrinho;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** A cada 3 unidades de um mesmo item, uma sai de graça. */
@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(CupomContexto ctx) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemCarrinho item : ctx.itens()) {
            int gratis = item.quantidade() / 3;
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
        }
        return total;
    }
}
