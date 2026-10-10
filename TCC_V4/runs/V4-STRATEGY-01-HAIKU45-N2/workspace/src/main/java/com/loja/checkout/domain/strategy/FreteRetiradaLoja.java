package com.loja.checkout.domain.strategy;

import com.loja.checkout.dto.Item;
import java.util.List;

public class FreteRetiradaLoja implements FreteCalculador {
    @Override
    public double calcularFrete(List<Item> itens) {
        return 0.0;
    }

    @Override
    public int getPrazoEntregaDias() {
        return 1;
    }

    @Override
    public boolean ehDisponivel(List<Item> itens) {
        return true;
    }
}
