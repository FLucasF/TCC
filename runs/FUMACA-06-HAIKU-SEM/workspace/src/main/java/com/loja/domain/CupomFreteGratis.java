package com.loja.domain;

import com.loja.dto.Item;
import java.util.List;

public class CupomFreteGratis extends Cupom {
    public CupomFreteGratis() {
        super("FRETEGRATIS");
    }

    @Override
    public boolean ehAplicavel(Double subtotalProdutos, Double totalComFrete) {
        return true;
    }

    @Override
    public CupomResultado calcular(Double subtotalProdutos, Double frete, List<Item> itens) {
        Double desconto = Arredondamento.arredondar(frete);
        return new CupomResultado(desconto, 0.0);
    }
}
