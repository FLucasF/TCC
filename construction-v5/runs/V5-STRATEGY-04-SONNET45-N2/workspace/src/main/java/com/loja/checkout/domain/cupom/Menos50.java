package com.loja.checkout.domain.cupom;

import com.loja.checkout.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class Menos50 implements Cupom {
    private static final BigDecimal DESCONTO_FIXO = new BigDecimal("50.00");
    private static final BigDecimal VALOR_MINIMO = new BigDecimal("300.00");

    @Override
    public boolean ehAplicavel(List<ItemCarrinho> itens, BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(VALOR_MINIMO) >= 0;
    }

    @Override
    public BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal subtotalProdutos, BigDecimal valorFrete) {
        return DESCONTO_FIXO;
    }
}
