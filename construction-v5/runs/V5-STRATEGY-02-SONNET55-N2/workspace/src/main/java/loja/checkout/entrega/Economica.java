package loja.checkout.entrega;

import org.springframework.stereotype.Component;

@Component
public class Economica extends PorPeso {
    public Economica() {
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
