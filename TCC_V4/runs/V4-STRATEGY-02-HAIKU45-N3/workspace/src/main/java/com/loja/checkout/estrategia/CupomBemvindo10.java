package com.loja.checkout.estrategia;

import java.util.List;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.util.Arredondamento;

public class CupomBemvindo10 implements EstrategiaCupom {
    @Override
    public double calcularDesconto(double subtotalProdutos, double frete, List<ItemRequest> itens) {
        return Arredondamento.arredondar(subtotalProdutos * 0.10);
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
