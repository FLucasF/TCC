package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EntregaRegistry {

    private final Map<String, ModalidadeEntrega> modalidades;

    public EntregaRegistry(List<ModalidadeEntrega> modalidades) {
        this.modalidades = modalidades.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::getCodigo, Function.identity()));
    }

    public ModalidadeEntrega buscar(String codigo) {
        return modalidades.get(codigo);
    }
}
