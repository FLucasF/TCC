package com.loja.checkout.cupom;

import com.loja.checkout.domain.ItemPedido;
import com.loja.checkout.domain.PedidoContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class Leve3Pague2 implements Cupom {

    private static final int TAMANHO_DO_LOTE = 3;

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

    @Override
    public boolean aplicavel(PedidoContext contexto) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(PedidoContext contexto) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemPedido item : contexto.itens()) {
            int unidadesGratis = item.quantidade() / TAMANHO_DO_LOTE;
            desconto = desconto.add(item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis)));
        }
        return desconto;
    }
}
