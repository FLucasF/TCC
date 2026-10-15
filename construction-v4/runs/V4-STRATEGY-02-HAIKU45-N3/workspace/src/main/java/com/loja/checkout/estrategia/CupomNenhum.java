package com.loja.checkout.estrategia;

import java.util.List;
import com.loja.checkout.dto.ItemRequest;

public class CupomNenhum implements EstrategiaCupom {
    @Override
    public double calcularDesconto(double subtotalProdutos, double frete, List<ItemRequest> itens) {
        return 0.0;
    }

    @Override
    public boolean aplicavel(double subtotalProdutos, List<ItemRequest> itens) {
        return true;
    }

    @Override
    public double ajustarFretePorCupom(double frete) {
        return frete;
    }

    @Override
    public List<ItemRequest> ajustarItensPorCupom(List<ItemRequest> itens) {
        return itens;
    }
}
