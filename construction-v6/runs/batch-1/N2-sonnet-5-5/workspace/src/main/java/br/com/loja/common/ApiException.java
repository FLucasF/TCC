package br.com.loja.common;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;

    public ApiException(ErrorCode code) {
        this(code, code.status());
    }

    public ApiException(ErrorCode code, HttpStatus status) {
        super(code.name(), null, false, false);
        this.code = code;
        this.status = status;
    }

    public ErrorCode code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }
}
