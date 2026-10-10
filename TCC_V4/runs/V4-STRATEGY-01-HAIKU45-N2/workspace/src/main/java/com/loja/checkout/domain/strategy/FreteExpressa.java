package com.loja.checkout.domain.strategy;

import com.loja.checkout.dto.Item;
import com.loja.checkout.util.Arredondador;
import java.util.List;

public class FreteExpressa implements FreteCalculador {
    @Override
    public double calcularFrete(List<Item> itens) {
        double pesoTotal = calcularPesoTotal(itens);
        double frete = 25.0 + (4.5 * pesoTotal);
        return Arredondador.arredondarParaCentavos(frete);
    }

    @Override
    public int getPrazoEntregaDias() {
        return 2;
    }

    @Override
    public boolean ehDisponivel(List<Item> itens) {
        return true;
    }

    private double calcularPesoTotal(List<Item> itens) {
        return itens.stream()
                .mapToDouble(item -> item.getPesoKg() * item.getQuantidade())
                .sum();
    }
}
