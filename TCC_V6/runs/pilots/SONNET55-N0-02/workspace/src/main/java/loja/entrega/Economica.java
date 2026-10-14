package loja.entrega;

import org.springframework.stereotype.Component;

@Component
public class Economica extends EntregaViaTransportadora {

    public Economica(Transportadora transportadora) {
        super(transportadora);
    }

    @Override
    public String codigo() {
        return "ECONOMICA";
    }
}
