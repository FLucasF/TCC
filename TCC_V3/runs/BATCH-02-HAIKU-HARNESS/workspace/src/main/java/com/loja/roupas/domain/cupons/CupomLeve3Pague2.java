package com.loja.roupas.domain.cupons;

import com.loja.roupas.domain.Cupom;
import com.loja.roupas.domain.ItemPedido;
import java.math.BigDecimal;
import java.util.List;

public class CupomLeve3Pague2 implements Cupom {
    @Override
    public boolean podeAplicar(BigDecimal subtotal, Double pesoTotal, List<ItemPedido> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, Double pesoTotal, BigDecimal frete, List<ItemPedido> itens) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (ItemPedido item : itens) {
            int unidadesGratis = item.getQuantidade() / 3;
            BigDecimal descontoItem = item.getPrecoUnitario().multiply(new BigDecimal(unidadesGratis));
            desconto = desconto.add(descontoItem);
        }

        return desconto;
    }
}
