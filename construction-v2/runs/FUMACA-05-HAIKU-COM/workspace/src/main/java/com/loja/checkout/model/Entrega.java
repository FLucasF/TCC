package com.loja.checkout.model;

public interface Entrega {

    double calcularFrete(double pesoKg);

    int getPrazo();

    boolean isDisponivel(double pesoKg);

    static Entrega obter(String modalidade) {
        return switch (modalidade) {
            case "ECONOMICA" -> new EntregaEconomica();
            case "EXPRESSA" -> new EntregaExpressa();
            case "RETIRADA_LOJA" -> new EntregaRetiradaLoja();
            case "MOTOBOY" -> new EntregaMotoboy();
            default -> null;
        };
    }

}

class EntregaEconomica implements Entrega {
    @Override
    public double calcularFrete(double pesoKg) {
        return 12.00 + (2.00 * pesoKg);
    }

    @Override
    public int getPrazo() {
        return 7;
    }

    @Override
    public boolean isDisponivel(double pesoKg) {
        return true;
    }
}

class EntregaExpressa implements Entrega {
    @Override
    public double calcularFrete(double pesoKg) {
        return 25.00 + (4.50 * pesoKg);
    }

    @Override
    public int getPrazo() {
        return 2;
    }

    @Override
    public boolean isDisponivel(double pesoKg) {
        return true;
    }
}

class EntregaRetiradaLoja implements Entrega {
    @Override
    public double calcularFrete(double pesoKg) {
        return 0.00;
    }

    @Override
    public int getPrazo() {
        return 1;
    }

    @Override
    public boolean isDisponivel(double pesoKg) {
        return true;
    }
}

class EntregaMotoboy implements Entrega {
    @Override
    public double calcularFrete(double pesoKg) {
        return 18.00;
    }

    @Override
    public int getPrazo() {
        return 0;
    }

    @Override
    public boolean isDisponivel(double pesoKg) {
        return pesoKg <= 5.0;
    }
}
