package com.loja.checkout.estrategia.cupom;

import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.util.Moeda;

import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2 implements Cupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        BigDecimal descontoTotal = BigDecimal.ZERO;

        for (ItemCarrinho item : itens) {
            int quantidade = item.getQuantidade();
            int gruposCompletos = quantidade / 3;
            BigDecimal descontoItem = item.getPrecoUnitario().multiply(BigDecimal.valueOf(gruposCompletos));
            descontoTotal = descontoTotal.add(descontoItem);
        }

        return Moeda.arredondar(descontoTotal);
    }

    @Override
    public boolean eAplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
