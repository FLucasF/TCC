package com.loja.domain.cupom;

import com.loja.model.ItemCarrinho;
import com.loja.util.Moeda;
import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2 implements Cupom {
    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        return true;
    }

    @Override
    public ResultadoDesconto calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        BigDecimal descontoTotal = new BigDecimal("0.00");

        for (ItemCarrinho item : itens) {
            int quantidade = item.getQuantidade();
            int unidadesGratis = quantidade / 3;
            BigDecimal descontoItem = item.getPrecoUnitario()
                .multiply(BigDecimal.valueOf(unidadesGratis));
            descontoTotal = descontoTotal.add(descontoItem);
        }

        return new ResultadoDesconto(Moeda.arredondar(descontoTotal));
    }
}
