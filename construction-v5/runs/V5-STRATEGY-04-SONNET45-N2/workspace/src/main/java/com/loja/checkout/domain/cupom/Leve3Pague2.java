package com.loja.checkout.domain.cupom;

import com.loja.checkout.model.ItemCarrinho;
import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2 implements Cupom {
    @Override
    public boolean ehAplicavel(List<ItemCarrinho> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal subtotalProdutos, BigDecimal valorFrete) {
        BigDecimal descontoTotal = BigDecimal.ZERO;

        for (ItemCarrinho item : itens) {
            int quantidade = item.getQuantidade();
            int unidadesGratis = quantidade / 3;

            if (unidadesGratis > 0) {
                BigDecimal descontoItem = item.getPrecoUnitario()
                    .multiply(new BigDecimal(unidadesGratis));
                descontoTotal = descontoTotal.add(descontoItem);
            }
        }

        return Arredondamento.arredondar(descontoTotal);
    }
}
