package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;

import java.math.BigDecimal;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graca. */
public final class Leve3Pague2 implements Cupom {

    private static final int UNIDADES_DO_LOTE = 3;

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.emCentavos(contexto.itens().stream()
                .map(this::unidadesGratis)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal unidadesGratis(Item item) {
        int gratis = item.quantidade() / UNIDADES_DO_LOTE;
        return Dinheiro.emCentavos(item.precoUnitario().multiply(BigDecimal.valueOf(gratis)));
    }
}
