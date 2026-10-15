package br.com.loja.pedidos.entrega;

import org.springframework.stereotype.Component;

@Component
public class Expressa extends ModalidadeTransportadora {

    public Expressa(Transportadora transportadora) {
        super(transportadora, "EXPRESSA");
    }

    @Override
    public String codigo() {
        return "EXPRESSA";
    }
}
