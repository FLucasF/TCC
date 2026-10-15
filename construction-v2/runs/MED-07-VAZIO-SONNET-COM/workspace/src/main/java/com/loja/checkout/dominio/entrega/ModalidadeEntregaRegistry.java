package com.loja.checkout.dominio.entrega;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ModalidadeEntregaRegistry {

    private final Map<String, ModalidadeEntrega> modalidades;

    public ModalidadeEntregaRegistry() {
        this(List.of(new Economica(), new Expressa(), new RetiradaLoja(), new Motoboy()));
    }

    public ModalidadeEntregaRegistry(List<ModalidadeEntrega> modalidades) {
        this.modalidades = modalidades.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
    }

    public Optional<ModalidadeEntrega> buscar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(modalidades.get(codigo));
    }
}
