package com.loja.checkout;

public class ErroNegocioException extends RuntimeException {
    private final Erro erro;

    public ErroNegocioException(Erro erro) {
        super(erro.name());
        this.erro = erro;
    }

    public Erro erro() {
        return erro;
    }
}
