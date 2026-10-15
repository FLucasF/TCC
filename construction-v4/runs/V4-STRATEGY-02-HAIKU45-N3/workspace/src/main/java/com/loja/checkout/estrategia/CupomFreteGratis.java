package com.loja.checkout.estrategia;

import java.util.List;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.util.Arredondamento;

public class CupomFreteGratis implements EstrategiaCupom {
    @Override
    public double calcularDesconto(double subtotalProdutos, double frete, List<ItemRequest> itens) {
        return Arredondamento.arredondar(frete);
    }

    @Override
    public boolean aplicavel(double subtotalProdutos, List<ItemRequest> itens) {
        return true;
    }

    @Override
    public double ajustarFretePorCupom(double frete) {
        return 0.0;
    }

    @Override
    public List<ItemRequest> ajustarItensPorCupom(List<ItemRequest> itens) {
        return itens;
    }
}
