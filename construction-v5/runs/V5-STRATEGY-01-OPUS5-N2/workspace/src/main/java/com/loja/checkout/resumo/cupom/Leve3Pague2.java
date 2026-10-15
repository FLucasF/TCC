package com.loja.checkout.resumo.cupom;

import com.loja.checkout.resumo.Dinheiro;
import com.loja.checkout.resumo.Item;
import java.math.BigDecimal;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graça. */
public class Leve3Pague2 implements Cupom {

    @Override
    public BigDecimal desconto(BaseCupom base) {
        BigDecimal desconto = base.itens().stream()
                .map(this::unidadesGratis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.centavos(desconto);
    }

    private BigDecimal unidadesGratis(Item item) {
        return item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3));
    }
}
