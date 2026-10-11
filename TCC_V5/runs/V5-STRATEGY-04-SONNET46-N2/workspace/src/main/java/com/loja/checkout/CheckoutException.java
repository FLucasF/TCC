package com.loja.checkout;

public class CheckoutException extends RuntimeException {

    private final ErroCode erroCode;

    public CheckoutException(ErroCode erroCode) {
        super(erroCode.name());
        this.erroCode = erroCode;
    }

    public ErroCode getErroCode() {
        return erroCode;
    }
}
