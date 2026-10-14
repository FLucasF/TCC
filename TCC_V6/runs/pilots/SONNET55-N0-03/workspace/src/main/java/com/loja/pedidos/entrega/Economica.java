package com.loja.pedidos.entrega;

import org.springframework.stereotype.Component;

@Component
public class Economica extends ModalidadeTransLog {

    public Economica(TransLogClient client) {
        super(client, "STD");
    }

    @Override
    public String codigo() {
        return "ECONOMICA";
    }
}
