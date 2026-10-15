package br.loja.checkout.entrega;

import org.springframework.stereotype.Component;

@Component
class Economica extends PorPeso {
    Economica() {
        super("ECONOMICA", "12.00", "2.00", 7);
    }
}
