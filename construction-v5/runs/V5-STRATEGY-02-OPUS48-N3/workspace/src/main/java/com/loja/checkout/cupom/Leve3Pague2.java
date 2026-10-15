package com.loja.checkout.cupom;

import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.pedido.ItemPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** A cada 3 unidades de um mesmo item, uma sai de graça. */
@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(CupomContexto ctx) {
        return true;
    }

    @Override
    public BigDecimal desconto(CupomContexto ctx) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemPedido item : ctx.itens()) {
            int gratis = item.quantidade() / 3;
            if (gratis > 0) {
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
        }
        return Dinheiro.centavos(desconto);
    }
}
