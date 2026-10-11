package com.loja.checkout.delivery;

public class EntregaMotoboy implements ModalidadeEntrega {
    private static final double PESO_MAXIMO_KG = 5.0;

    @Override
    public double calcularFrete(double pesoTotalKg) {
        return 18.00;
    }

    @Override
    public int getPrazoEntregaDias() {
        return 0;
    }

    @Override
    public boolean estaDisponivel(double pesoTotalKg) {
        return pesoTotalKg <= PESO_MAXIMO_KG;
    }

    @Override
    public String getNome() {
        return "MOTOBOY";
    }
}
