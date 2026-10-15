package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Leve3Pague2 implements Cupom {

    private static final int UNIDADES_PARA_UMA_GRATIS = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.arredondar(contexto.pedido().itens().stream()
                .map(this::unidadesGratis)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal unidadesGratis(ItemPedido item) {
        int gratis = item.quantidade() / UNIDADES_PARA_UMA_GRATIS;
        return item.precoUnitario().multiply(BigDecimal.valueOf(gratis));
    }
}
