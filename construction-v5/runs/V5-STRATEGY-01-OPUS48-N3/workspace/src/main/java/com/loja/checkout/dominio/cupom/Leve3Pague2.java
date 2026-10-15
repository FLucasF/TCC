package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ItemPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * A cada 3 unidades de um mesmo item, uma sai de graça. O desconto é o preço das
 * unidades grátis somado item a item.
 */
@Component
public class Leve3Pague2 implements Cupom {

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
