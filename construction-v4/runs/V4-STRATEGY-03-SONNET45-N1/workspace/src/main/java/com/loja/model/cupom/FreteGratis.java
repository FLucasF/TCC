package com.loja.model.cupom;

import com.loja.dto.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public class FreteGratis implements Cupom {
    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemCarrinho> itens, BigDecimal frete) {
        return frete;
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
