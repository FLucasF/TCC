package br.com.loja.pedidos.comum;

import org.springframework.http.HttpStatus;

/** Recusa de uma operação, devolvida ao site como {@code { "erro": "CODIGO" }}. */
public class ErroNegocio extends RuntimeException {

    private final String codigo;
    private final HttpStatus status;

    public ErroNegocio(String codigo, HttpStatus status) {
        super(codigo);
        this.codigo = codigo;
        this.status = status;
    }

    public static ErroNegocio invalido(String codigo) {
        return new ErroNegocio(codigo, HttpStatus.UNPROCESSABLE_CONTENT);
    }

    public static ErroNegocio naoEncontrado(String codigo) {
        return new ErroNegocio(codigo, HttpStatus.NOT_FOUND);
    }

    public static ErroNegocio conflito(String codigo) {
        return new ErroNegocio(codigo, HttpStatus.CONFLICT);
    }

    public String codigo() {
        return codigo;
    }

    public HttpStatus status() {
        return status;
    }
}
