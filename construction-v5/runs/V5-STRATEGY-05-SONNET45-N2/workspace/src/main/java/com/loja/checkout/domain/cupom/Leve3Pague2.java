package com.loja.checkout.domain.cupom;

import com.loja.checkout.model.request.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public class Leve3Pague2 implements Cupom {

    @Override
    public boolean eAplicavel(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens) {
        BigDecimal descontoTotal = BigDecimal.ZERO;

        for (ItemRequest item : itens) {
            int quantidade = item.getQuantidade();
            int unidadesGratis = quantidade / 3;

            if (unidadesGratis > 0) {
                BigDecimal descontoItem = item.getPrecoUnitario()
                    .multiply(BigDecimal.valueOf(unidadesGratis));
                descontoTotal = descontoTotal.add(descontoItem);
            }
        }

        return descontoTotal;
    }
}
