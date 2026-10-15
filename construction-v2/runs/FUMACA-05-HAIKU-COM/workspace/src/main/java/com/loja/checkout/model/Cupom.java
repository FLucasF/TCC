package com.loja.checkout.model;

import com.loja.checkout.util.Arredondador;

public interface Cupom {

    double calcularDesconto(double subtotalProdutos);

    boolean isAplicavel(double subtotalProdutos);

    static Cupom obter(String codigo) {
        return switch (codigo) {
            case "BEMVINDO10" -> new CupomBemvindo10();
            case "MENOS50" -> new CupomMenos50();
            case "FRETEGRATIS" -> new CupomFreteGratis();
            case "LEVE3PAGUE2" -> new CupomLeve3Pague2();
            default -> null;
        };
    }

}

class CupomBemvindo10 implements Cupom {
    @Override
    public double calcularDesconto(double subtotalProdutos) {
        return Arredondador.arredondar(subtotalProdutos * 0.10);
    }

    @Override
    public boolean isAplicavel(double subtotalProdutos) {
        return true;
    }
}

class CupomMenos50 implements Cupom {
    @Override
    public double calcularDesconto(double subtotalProdutos) {
        return 50.00;
    }

    @Override
    public boolean isAplicavel(double subtotalProdutos) {
        return subtotalProdutos >= 300.00;
    }
}

class CupomFreteGratis implements Cupom {
    @Override
    public double calcularDesconto(double subtotalProdutos) {
        return 0.00;
    }

    @Override
    public boolean isAplicavel(double subtotalProdutos) {
        return true;
    }
}

class CupomLeve3Pague2 implements Cupom {
    @Override
    public double calcularDesconto(double subtotalProdutos) {
        return 0.00;
    }

    @Override
    public boolean isAplicavel(double subtotalProdutos) {
        return true;
    }
}
