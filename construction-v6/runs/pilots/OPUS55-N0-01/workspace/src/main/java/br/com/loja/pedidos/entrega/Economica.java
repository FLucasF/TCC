package br.com.loja.pedidos.entrega;

import org.springframework.stereotype.Component;

@Component
public class Economica extends ModalidadeTransportadora {

    public Economica(Transportadora transportadora) {
        super(transportadora, "ECONOMICA");
    }

    @Override
    public String codigo() {
        return "ECONOMICA";
    }
}
