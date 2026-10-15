package com.loja.checkout.domain.cupom;

import com.loja.checkout.model.ItemCarrinho;
import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;
import java.util.List;

public class BemVindo10 implements Cupom {
    private static final BigDecimal PERCENTUAL_DESCONTO = new BigDecimal("0.10");

    @Override
    public boolean ehAplicavel(List<ItemCarrinho> itens, BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal subtotalProdutos, BigDecimal valorFrete) {
        BigDecimal desconto = subtotalProdutos.multiply(PERCENTUAL_DESCONTO);
        return Arredondamento.arredondar(desconto);
    }
}
