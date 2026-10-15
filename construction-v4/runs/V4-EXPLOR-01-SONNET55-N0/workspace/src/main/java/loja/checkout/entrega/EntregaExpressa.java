package loja.checkout.entrega;

import org.springframework.stereotype.Component;

@Component
class EntregaExpressa extends EntregaPorPeso {
    EntregaExpressa() {
        super("EXPRESSA", 2, "25.00", "4.50");
    }
}
