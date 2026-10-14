package com.loja.pedidos.entrega;

import org.springframework.stereotype.Component;

@Component
public class Expressa extends ModalidadeTransLog {

    public Expressa(TransLogClient client) {
        super(client, "EXP");
    }

    @Override
    public String codigo() {
        return "EXPRESSA";
    }
}
