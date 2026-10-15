package com.loja.checkout.cupom.impl;

import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.domain.ItemPedido;
import com.loja.checkout.domain.PedidoContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Leve3Pague2Cupom implements Cupom {

    private static final int TAMANHO_LEVA = 3;

    @Override
    public String getCodigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(PedidoContext pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(PedidoContext pedido, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemPedido item : pedido.getItens()) {
            int itensGratis = item.quantidade() / TAMANHO_LEVA;
            if (itensGratis > 0) {
                desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(itensGratis)));
            }
        }
        return desconto;
    }
}
