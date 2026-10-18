package br.com.loja.pedidos.common;

import org.springframework.http.HttpStatus;

public class DomainException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public DomainException(String code, HttpStatus status) {
        super(code);
        this.code = code;
        this.status = status;
    }

    public static DomainException unprocessable(String code) {
        return new DomainException(code, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static DomainException conflict(String code) {
        return new DomainException(code, HttpStatus.CONFLICT);
    }

    public static DomainException notFound(String code) {
        return new DomainException(code, HttpStatus.NOT_FOUND);
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }
}
