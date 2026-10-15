package com.loja.checkout.estrategia;

public interface EstrategiaEntrega {
    double calcularFrete(double pesoKg);
    int prazoEntregaDias();
    boolean disponivel(double pesoKg);
}
