package com.loja.orders.common;

import org.springframework.http.HttpStatus;

/** Recusa de uma operação: vira {@code { "error": "CODE" }} com a situação HTTP indicada. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode code;

    public ApiException(HttpStatus status, ErrorCode code) {
        super(code.name());
        this.status = status;
        this.code = code;
    }

    public static ApiException unprocessable(ErrorCode code) {
        return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, code);
    }

    public static ApiException notFound(ErrorCode code) {
        return new ApiException(HttpStatus.NOT_FOUND, code);
    }

    public static ApiException conflict(ErrorCode code) {
        return new ApiException(HttpStatus.CONFLICT, code);
    }

    public HttpStatus status() {
        return status;
    }

    public ErrorCode code() {
        return code;
    }
}
