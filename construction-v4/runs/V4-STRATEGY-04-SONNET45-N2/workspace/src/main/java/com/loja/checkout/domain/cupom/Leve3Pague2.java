package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Cupom;
import com.loja.checkout.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

import static com.loja.checkout.util.Arredondamento.arredondar;

public class Leve3Pague2 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        BigDecimal desconto = new BigDecimal("0.00");

        for (ItemCarrinho item : itens) {
            int unidadesGratis = item.quantidade() / 3;
            BigDecimal descontoItem = item.precoUnitario()
                .multiply(BigDecimal.valueOf(unidadesGratis));
            desconto = desconto.add(descontoItem);
        }

        return arredondar(desconto);
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
