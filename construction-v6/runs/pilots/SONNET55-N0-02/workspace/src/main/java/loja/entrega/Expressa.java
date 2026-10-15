package loja.entrega;

import org.springframework.stereotype.Component;

@Component
public class Expressa extends EntregaViaTransportadora {

    public Expressa(Transportadora transportadora) {
        super(transportadora);
    }

    @Override
    public String codigo() {
        return "EXPRESSA";
    }
}
