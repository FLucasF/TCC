package com.loja.checkout.domain.cupom;

public record ResultadoCupom(double desconto, double freteOriginal) {
    public ResultadoCupom(double desconto) {
        this(desconto, 0.0);
    }
}
