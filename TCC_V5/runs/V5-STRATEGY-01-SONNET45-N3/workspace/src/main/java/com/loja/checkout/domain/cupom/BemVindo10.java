package com.loja.checkout.domain.cupom;

import com.loja.checkout.service.ArredondamentoUtil;
import java.math.BigDecimal;
import java.util.List;

public class BemVindo10 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCupom> itens) {
        BigDecimal desconto = subtotalProdutos.multiply(new BigDecimal("0.10"));
        return ArredondamentoUtil.arredondar(desconto);
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
