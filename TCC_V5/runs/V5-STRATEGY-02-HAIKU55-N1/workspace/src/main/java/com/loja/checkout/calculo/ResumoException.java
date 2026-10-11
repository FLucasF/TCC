package com.loja.checkout.calculo;

public class ResumoException extends RuntimeException {

    private final Erro erro;

    public ResumoException(Erro erro) {
        super(erro.name());
        this.erro = erro;
    }

    public Erro erro() {
        return erro;
    }
}
