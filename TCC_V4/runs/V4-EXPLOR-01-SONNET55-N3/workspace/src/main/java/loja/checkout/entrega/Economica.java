package loja.checkout.entrega;

import org.springframework.stereotype.Component;

@Component
class Economica extends EntregaPorPeso {
    Economica() {
        super("12.00", "2.00");
    }

    @Override
    public String codigo() {
        return "ECONOMICA";
    }

    @Override
    public int prazoDias() {
        return 7;
    }
}
