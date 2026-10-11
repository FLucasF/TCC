package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.ItemPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** A cada 3 unidades de um mesmo item do carrinho, uma sai de graça. */
@Component
public class Leve3Pague2 implements Cupom {

    private static final int UNIDADES_POR_GRATIS = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal valorAbatido(ContextoCupom contexto) {
        return contexto.carrinho().itens().stream()
                .map(this::valorDasUnidadesGratis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal valorDasUnidadesGratis(ItemPedido item) {
        int gratis = item.quantidade() / UNIDADES_POR_GRATIS;
        return item.precoUnitario().multiply(BigDecimal.valueOf(gratis));
    }
}
