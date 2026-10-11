package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graca. */
@Component
public class CupomLeve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemPedido item : contexto.itens()) {
            int gratuitas = item.quantidade() / 3;
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(gratuitas)));
        }
        return Dinheiro.centavos(desconto);
    }
}
