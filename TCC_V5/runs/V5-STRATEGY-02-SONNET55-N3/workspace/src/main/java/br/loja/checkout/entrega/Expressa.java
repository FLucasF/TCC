package br.loja.checkout.entrega;

import org.springframework.stereotype.Component;

@Component
class Expressa extends PorPeso {
    Expressa() {
        super("EXPRESSA", "25.00", "4.50", 2);
    }
}
