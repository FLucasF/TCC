package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

/** R$ 12,00 + R$ 2,00 por kg, em 7 dias. */
@Component
class Economica extends ModalidadePorPeso {

    Economica() {
        super("ECONOMICA", "12.00", "2.00", 7);
    }
}
