package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ItemPedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graça. */
@Component
public class CupomLeve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(ContextoCupom ctx) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : ctx.itens()) {
            int gratis = item.quantidade() / 3;
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
        }
        return total;
    }
}
