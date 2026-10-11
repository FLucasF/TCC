package com.loja.checkout.service.entrega;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ModalidadeEntregaRegistry {

    private final Map<String, ModalidadeEntrega> modalidades;

    public ModalidadeEntregaRegistry(List<ModalidadeEntrega> modalidades) {
        this.modalidades = modalidades.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::getCodigo, Function.identity()));
    }

    public ModalidadeEntrega buscar(String codigo) {
        return modalidades.get(codigo);
    }
}
