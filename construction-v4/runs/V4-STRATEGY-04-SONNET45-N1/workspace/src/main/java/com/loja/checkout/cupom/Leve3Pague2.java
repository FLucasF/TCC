package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.util.Arredondamento;

import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2 implements CupomStrategy {
    @Override
    public boolean ehAplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        BigDecimal descontoTotal = BigDecimal.ZERO;

        for (ItemCarrinho item : itens) {
            int quantidade = item.getQuantidade();
            int unidadesGratis = quantidade / 3;

            if (unidadesGratis > 0) {
                BigDecimal descontoItem = item.getPrecoUnitario()
                        .multiply(BigDecimal.valueOf(unidadesGratis));
                descontoTotal = descontoTotal.add(descontoItem);
            }
        }

        return Arredondamento.arredondar(descontoTotal);
    }
}
