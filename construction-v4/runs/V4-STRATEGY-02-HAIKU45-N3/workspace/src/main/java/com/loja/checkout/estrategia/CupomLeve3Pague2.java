package com.loja.checkout.estrategia;

import java.util.ArrayList;
import java.util.List;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.util.Arredondamento;

public class CupomLeve3Pague2 implements EstrategiaCupom {
    @Override
    public double calcularDesconto(double subtotalProdutos, double frete, List<ItemRequest> itens) {
        double desconto = 0.0;
        for (ItemRequest item : itens) {
            int quantidadeGratis = item.quantidade() / 3;
            double descontoItem = quantidadeGratis * item.precoUnitario();
            desconto += descontoItem;
        }
        return Arredondamento.arredondar(desconto);
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
