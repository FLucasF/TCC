package br.loja.checkout.entrega;

import org.springframework.stereotype.Component;

@Component
class Expressa extends FretePorPeso {
    Expressa() {
        super("EXPRESSA", 2, "25.00", "4.50");
    }
}
