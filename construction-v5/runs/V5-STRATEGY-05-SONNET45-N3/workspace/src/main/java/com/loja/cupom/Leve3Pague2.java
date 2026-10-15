package com.loja.cupom;

import com.loja.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class Leve3Pague2 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        return itens.stream()
            .map(item -> {
                int unidadesGratis = item.quantidade() / 3;
                return item.precoUnitario()
                    .multiply(BigDecimal.valueOf(unidadesGratis));
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean podeAplicar(BigDecimal subtotalProdutos) {
        return true;
    }
}
