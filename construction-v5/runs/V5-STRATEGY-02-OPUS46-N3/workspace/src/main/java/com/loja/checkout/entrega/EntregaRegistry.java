package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EntregaRegistry {

    private final Map<String, ModalidadeEntrega> modalidades;

    public EntregaRegistry(List<ModalidadeEntrega> lista) {
        this.modalidades = lista.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
    }

    public ModalidadeEntrega buscar(String codigo) {
        return codigo == null ? null : modalidades.get(codigo);
    }
}
