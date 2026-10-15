package com.loja.model.cupom;

import com.loja.dto.ItemCarrinho;
import com.loja.util.Dinheiro;

import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2 implements Cupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemCarrinho> itens, BigDecimal frete) {
        BigDecimal descontoTotal = new BigDecimal("0.00");

        for (ItemCarrinho item : itens) {
            int unidadesGratis = item.quantidade() / 3;
            BigDecimal descontoItem = item.precoUnitario().multiply(BigDecimal.valueOf(unidadesGratis));
            descontoTotal = descontoTotal.add(descontoItem);
        }

        return Dinheiro.arredondar(descontoTotal);
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
