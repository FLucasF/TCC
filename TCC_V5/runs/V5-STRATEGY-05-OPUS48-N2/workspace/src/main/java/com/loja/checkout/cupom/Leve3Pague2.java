package com.loja.checkout.cupom;

import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.pedido.ItemPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * A cada 3 unidades de um mesmo item do carrinho, uma sai de graça.
 */
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
    public BigDecimal desconto(ContextoCupom ctx) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : ctx.itens()) {
            int gratis = item.quantidade() / 3;
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
        }
        return Dinheiro.centavos(total);
    }
}
