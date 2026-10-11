package com.loja.checkout.domain.cupom;

import com.loja.checkout.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class FreteGratis implements Cupom {
    @Override
    public boolean ehAplicavel(List<ItemCarrinho> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal subtotalProdutos, BigDecimal valorFrete) {
        return valorFrete;
    }
}
