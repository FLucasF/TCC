package com.loja.service.estrategia;

import com.loja.domain.Item;
import com.loja.util.Arredondador;
import java.math.BigDecimal;
import java.util.List;

public class CupomLeve3Pague2 implements CalculoCupom {
    private List<Item> itens;

    public CupomLeve3Pague2(List<Item> itens) {
        this.itens = itens;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete) {
        BigDecimal desconto = BigDecimal.ZERO;

        for (Item item : itens) {
            if (item.quantidade >= 3) {
                int unidadesGratis = item.quantidade / 3;
                BigDecimal descontoItem = item.precoUnitario.multiply(new BigDecimal(unidadesGratis));
                desconto = desconto.add(descontoItem);
            }
        }

        return Arredondador.arredondarParaCentavos(desconto);
    }

    @Override
    public BigDecimal calcularDescontoFrete(BigDecimal frete) {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
