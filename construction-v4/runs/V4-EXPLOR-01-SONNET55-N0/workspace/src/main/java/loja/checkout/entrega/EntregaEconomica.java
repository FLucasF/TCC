package loja.checkout.entrega;

import org.springframework.stereotype.Component;

@Component
class EntregaEconomica extends EntregaPorPeso {
    EntregaEconomica() {
        super("ECONOMICA", 7, "12.00", "2.00");
    }
}
