package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemRequest;
import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2 implements Cupom {
    private List<ItemRequest> itensCache;

    public void setItens(List<ItemRequest> itens) {
        this.itensCache = itens;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete) {
        if (itensCache == null) {
            return BigDecimal.ZERO;
        }
        return itensCache.stream()
            .map(item -> {
                int unidadesGratis = item.quantidade() / 3;
                return item.precoUnitario().multiply(new BigDecimal(unidadesGratis));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public boolean verificarAplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
