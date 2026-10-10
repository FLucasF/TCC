package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.PedidoRequest;
import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;

public class Leve3Pague2 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(ContextoCupom contexto) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (PedidoRequest.ItemCarrinho item : contexto.itens()) {
            int quantidadeGratis = item.quantidade() / 3;
            BigDecimal descontoItem = BigDecimal.valueOf(item.precoUnitario())
                .multiply(BigDecimal.valueOf(quantidadeGratis));
            desconto = desconto.add(descontoItem);
        }

        return Dinheiro.arredondar(desconto);
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }
}
