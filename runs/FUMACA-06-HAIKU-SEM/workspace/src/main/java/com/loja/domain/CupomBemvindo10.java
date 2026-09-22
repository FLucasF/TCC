package com.loja.domain;

import com.loja.dto.Item;
import java.util.List;

public class CupomBemvindo10 extends Cupom {
    public CupomBemvindo10() {
        super("BEMVINDO10");
    }

    @Override
    public boolean ehAplicavel(Double subtotalProdutos, Double totalComFrete) {
        return true;
    }

    @Override
    public CupomResultado calcular(Double subtotalProdutos, Double frete, List<Item> itens) {
        Double desconto = Arredondamento.arredondar(subtotalProdutos * 0.10);
        return new CupomResultado(desconto, frete);
    }
}
