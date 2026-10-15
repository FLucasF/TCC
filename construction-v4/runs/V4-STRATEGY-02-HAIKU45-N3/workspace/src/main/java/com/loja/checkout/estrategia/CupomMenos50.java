package com.loja.checkout.estrategia;

import java.util.List;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.util.Arredondamento;

public class CupomMenos50 implements EstrategiaCupom {
    @Override
    public double calcularDesconto(double subtotalProdutos, double frete, List<ItemRequest> itens) {
        return Arredondamento.arredondar(50.00);
    }

    @Override
    public boolean aplicavel(double subtotalProdutos, List<ItemRequest> itens) {
        return subtotalProdutos >= 300.00;
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
