package com.loja.checkout.domain.strategy;

import com.loja.checkout.dto.Item;
import com.loja.checkout.util.Arredondador;
import java.util.List;

public class FreteEconomica implements FreteCalculador {
    @Override
    public double calcularFrete(List<Item> itens) {
        double pesoTotal = calcularPesoTotal(itens);
        double frete = 12.0 + (2.0 * pesoTotal);
        return Arredondador.arredondarParaCentavos(frete);
    }

    @Override
    public int getPrazoEntregaDias() {
        return 7;
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
