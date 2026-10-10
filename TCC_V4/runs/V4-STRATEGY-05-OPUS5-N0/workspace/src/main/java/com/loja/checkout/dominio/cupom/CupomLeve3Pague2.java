package com.loja.checkout.dominio.cupom;

import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graca. */
@Component
public class CupomLeve3Pague2 implements Cupom {

    private static final int UNIDADES_POR_GRUPO = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemPedido item : contexto.itens()) {
            int gratis = item.quantidade() / UNIDADES_POR_GRUPO;
            desconto = desconto.add(
                    item.precoUnitario().multiply(BigDecimal.valueOf(gratis), Dinheiro.CALCULO));
        }
        return Dinheiro.valor(desconto);
    }
}
