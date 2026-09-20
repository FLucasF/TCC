package com.loja.payment;

import java.util.HashMap;
import java.util.Map;

public class PaymentFormRegistry {
    private static final Map<String, PaymentForm> forms = new HashMap<>();

    static {
        forms.put("PIX", new PixForm());
        forms.put("BOLETO", new BoletoForm());
        forms.put("CARTAO", new CartaoForm());
    }

    public static PaymentForm getForm(String code) {
        return forms.get(code);
    }
}
