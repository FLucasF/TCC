package com.loja.checkout.servico;

import com.loja.checkout.dominio.entrega.*;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class ModalidadeEntregaFactory {

    private final Map<String, ModalidadeEntrega> modalidades = new HashMap<>();

    public ModalidadeEntregaFactory() {
        modalidades.put("ECONOMICA", new Economica());
        modalidades.put("EXPRESSA", new Expressa());
        modalidades.put("RETIRADA_LOJA", new RetiradaLoja());
        modalidades.put("MOTOBOY", new Motoboy());
    }

    public ModalidadeEntrega obter(String codigo) {
        return modalidades.get(codigo);
    }
}
