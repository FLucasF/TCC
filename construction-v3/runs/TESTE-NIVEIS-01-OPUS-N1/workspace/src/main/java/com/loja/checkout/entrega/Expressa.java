package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

/** R$ 25,00 + R$ 4,50 por kg, em 2 dias. */
@Component
class Expressa extends ModalidadePorPeso {

    Expressa() {
        super("EXPRESSA", "25.00", "4.50", 2);
    }
}
