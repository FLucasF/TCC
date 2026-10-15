package com.loja.domain.cupom;

import com.loja.model.ItemCarrinho;
import com.loja.util.Moeda;
import java.math.BigDecimal;
import java.util.List;

public class BemVindo10 implements Cupom {
    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        return true;
    }

    @Override
    public ResultadoDesconto calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        BigDecimal desconto = Moeda.arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
        return new ResultadoDesconto(desconto);
    }
}
