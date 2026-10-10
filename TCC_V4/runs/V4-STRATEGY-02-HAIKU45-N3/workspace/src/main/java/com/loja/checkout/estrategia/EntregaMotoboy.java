package com.loja.checkout.estrategia;

public class EntregaMotoboy implements EstrategiaEntrega {
    @Override
    public double calcularFrete(double pesoKg) {
        return 18.00;
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }

    @Override
    public boolean disponivel(double pesoKg) {
        return pesoKg <= 5.0;
    }
}
