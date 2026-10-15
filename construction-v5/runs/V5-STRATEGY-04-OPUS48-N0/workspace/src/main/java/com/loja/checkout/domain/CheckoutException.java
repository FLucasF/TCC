package com.loja.checkout.domain;

/**
 * Erro de negócio no cálculo do resumo. O {@code codigo} é exatamente o código
 * que o site espera receber em {@code {"erro": "CODIGO"}}.
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
