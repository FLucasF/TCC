package com.loja.checkout.domain.cupom;

import com.loja.checkout.service.ArredondamentoUtil;
import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCupom> itens) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (ItemCupom item : itens) {
            int unidadesGratis = item.quantidade() / 3;
            BigDecimal descontoItem = item.precoUnitario().multiply(new BigDecimal(unidadesGratis));
            desconto = desconto.add(descontoItem);
        }

        return ArredondamentoUtil.arredondar(desconto);
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
