package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graça. */
@Component
public class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(CupomContexto contexto) {
        return true;
    }

    @Override
    public BigDecimal desconto(CupomContexto contexto) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedido item : contexto.itens()) {
            int gratis = item.quantidade() / 3;
            if (gratis > 0) {
                total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
            }
        }
        return Dinheiro.centavos(total);
    }
}
