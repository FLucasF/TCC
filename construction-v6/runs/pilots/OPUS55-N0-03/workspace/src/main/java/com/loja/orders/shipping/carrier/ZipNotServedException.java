package com.loja.orders.shipping.carrier;

public class ZipNotServedException extends RuntimeException {

    public ZipNotServedException() {
        super("A transportadora não atende este CEP");
    }
}
