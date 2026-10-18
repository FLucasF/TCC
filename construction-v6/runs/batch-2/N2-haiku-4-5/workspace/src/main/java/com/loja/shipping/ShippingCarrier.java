package com.loja.shipping;

public interface ShippingCarrier {
    ShippingQuote getQuote(String zipCode, Double weightGrams);
}

