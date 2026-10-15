package com.loja.checkout.exception;

/**
 * Carrega o codigo de erro de negocio a ser devolvido ao site,
 * conforme a tabela de erros do checkout.
 */
public class CheckoutException extends RuntimeException {

    private final String codigo;

    public CheckoutException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
