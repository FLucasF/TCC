package br.loja.checkout.entrega;

import org.springframework.stereotype.Component;

@Component
class Economica extends FretePorPeso {
    Economica() {
        super("ECONOMICA", 7, "12.00", "2.00");
    }
}
