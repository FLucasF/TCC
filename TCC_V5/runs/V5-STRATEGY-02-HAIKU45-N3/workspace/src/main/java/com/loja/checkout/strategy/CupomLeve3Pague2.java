package com.loja.checkout.strategy;

import java.math.BigDecimal;
import com.loja.checkout.model.ItemRequest;
import com.loja.checkout.util.ArredondadorMeio;
import java.util.List;

public class CupomLeve3Pague2 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (ItemRequest item : itens) {
            if (item.getQuantidade() >= 3) {
                int quantidadeGratis = item.getQuantidade() / 3;
                BigDecimal precoItem = new BigDecimal(item.getPrecoUnitario().toString());
                desconto = desconto.add(precoItem.multiply(new BigDecimal(quantidadeGratis)));
            }
        }

        return ArredondadorMeio.arredondar(desconto);
    }

    @Override
    public boolean ehAplicavel(BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public boolean ofereceFretGratis() {
        return false;
    }

    @Override
    public String codigo() {
        return "LEVE3PAGUE2";
    }

}
