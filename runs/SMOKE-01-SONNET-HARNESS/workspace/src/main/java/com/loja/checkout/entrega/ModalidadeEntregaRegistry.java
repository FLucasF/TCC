package com.loja.checkout.entrega;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ModalidadeEntregaRegistry {

    private final Map<String, ModalidadeEntrega> porCodigo;

    public ModalidadeEntregaRegistry(List<ModalidadeEntrega> modalidades) {
        this.porCodigo = modalidades.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
    }

    public Optional<ModalidadeEntrega> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
