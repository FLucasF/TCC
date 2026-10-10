package com.loja.estrategia;

import com.loja.dto.ItemRequest;
import java.math.BigDecimal;
import java.util.List;

public class CupomLeve3Pague2 implements EstrategiaCupom {
    private List<ItemRequest> itens;

    public CupomLeve3Pague2(List<ItemRequest> itens) {
        this.itens = itens;
    }

    @Override
    public BigDecimal aplicar(BigDecimal subtotal, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            int quantidade = item.getQuantidade();
            int gratuitos = quantidade / 3;
            if (gratuitos > 0) {
                desconto = desconto.add(item.getPrecoUnitario().multiply(new BigDecimal(gratuitos)));
            }
        }
        return desconto;
    }

    @Override
    public boolean validar(BigDecimal subtotal) {
        return true;
    }
}
