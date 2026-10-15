package com.loja.domain;

import com.loja.dto.Item;
import java.util.List;

public class CupomMenos50 extends Cupom {
    public CupomMenos50() {
        super("MENOS50");
    }

    @Override
    public boolean ehAplicavel(Double subtotalProdutos, Double totalComFrete) {
        return subtotalProdutos >= 300.0;
    }

    @Override
    public CupomResultado calcular(Double subtotalProdutos, Double frete, List<Item> itens) {
        Double desconto = Arredondamento.arredondar(50.0);
        return new CupomResultado(desconto, frete);
    }
}
