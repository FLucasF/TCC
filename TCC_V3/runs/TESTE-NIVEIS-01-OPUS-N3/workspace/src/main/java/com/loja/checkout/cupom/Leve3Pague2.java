package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** A cada 3 unidades de um mesmo item, uma sai de graça. */
@Component
class Leve3Pague2 implements Cupom {

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        BigDecimal desconto = contexto.pedido().itens().stream()
                .map(this::unidadesGratis)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.centavos(desconto);
    }

    private BigDecimal unidadesGratis(ItemPedido item) {
        return item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade() / 3));
    }
}
