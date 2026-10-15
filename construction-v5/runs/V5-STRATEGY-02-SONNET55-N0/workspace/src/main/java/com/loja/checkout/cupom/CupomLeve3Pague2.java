package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CupomLeve3Pague2 implements Cupom {
    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(ContextoCupom ctx) {
        BigDecimal total = Dinheiro.ZERO;
        for (Item item : ctx.itens()) {
            total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3)));
        }
        return Dinheiro.arredondar(total);
    }
}
