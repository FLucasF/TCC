package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Cupom;
import com.loja.checkout.model.ItemCarrinho;
import com.loja.checkout.util.Dinheiro;

import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2 implements Cupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        BigDecimal descontoTotal = BigDecimal.ZERO;

        for (ItemCarrinho item : itens) {
            int unidadesGratis = item.quantidade() / 3;
            BigDecimal descontoItem = item.precoUnitario()
                .multiply(BigDecimal.valueOf(unidadesGratis));
            descontoTotal = descontoTotal.add(descontoItem);
        }

        return Dinheiro.arredondar(descontoTotal);
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        return true;
    }
}
