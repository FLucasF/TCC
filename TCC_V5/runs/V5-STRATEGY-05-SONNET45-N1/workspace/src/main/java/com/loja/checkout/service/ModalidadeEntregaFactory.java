package com.loja.checkout.service;

import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.entrega.*;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ModalidadeEntregaFactory {
    private final Map<String, ModalidadeEntrega> modalidades = Map.of(
        "ECONOMICA", new Economica(),
        "EXPRESSA", new Expressa(),
        "RETIRADA_LOJA", new RetiradaLoja(),
        "MOTOBOY", new Motoboy()
    );

    public ModalidadeEntrega obter(String codigo) {
        return modalidades.get(codigo);
    }

    public boolean existe(String codigo) {
        return modalidades.containsKey(codigo);
    }
}
