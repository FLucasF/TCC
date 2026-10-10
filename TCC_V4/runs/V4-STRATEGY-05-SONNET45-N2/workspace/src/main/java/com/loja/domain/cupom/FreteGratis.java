package com.loja.domain.cupom;

import com.loja.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class FreteGratis implements Cupom {
    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        return true;
    }

    @Override
    public ResultadoDesconto calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        return new ResultadoDesconto(frete);
    }
}
