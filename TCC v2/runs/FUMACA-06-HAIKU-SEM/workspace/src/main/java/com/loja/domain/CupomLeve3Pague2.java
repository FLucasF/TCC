package com.loja.domain;

import com.loja.dto.Item;
import java.util.List;

public class CupomLeve3Pague2 extends Cupom {
    public CupomLeve3Pague2() {
        super("LEVE3PAGUE2");
    }

    @Override
    public boolean ehAplicavel(Double subtotalProdutos, Double totalComFrete) {
        return true;
    }

    @Override
    public CupomResultado calcular(Double subtotalProdutos, Double frete, List<Item> itens) {
        Double desconto = 0.0;

        for (Item item : itens) {
            int gratis = item.getQuantidade() / 3;
            Double descontoItem = Arredondamento.arredondar(item.getPrecoUnitario() * gratis);
            desconto = Arredondamento.arredondar(desconto + descontoItem);
        }

        return new CupomResultado(desconto, frete);
    }
}
