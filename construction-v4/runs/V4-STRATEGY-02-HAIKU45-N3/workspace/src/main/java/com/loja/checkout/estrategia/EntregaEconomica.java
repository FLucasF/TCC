package com.loja.checkout.estrategia;

import com.loja.checkout.util.Arredondamento;

public class EntregaEconomica implements EstrategiaEntrega {
    @Override
    public double calcularFrete(double pesoKg) {
        return Arredondamento.arredondar(12.00 + (2.00 * pesoKg));
    }

    @Override
    public int prazoEntregaDias() {
        return 7;
    }

    @Override
    public boolean disponivel(double pesoKg) {
        return true;
    }
}
