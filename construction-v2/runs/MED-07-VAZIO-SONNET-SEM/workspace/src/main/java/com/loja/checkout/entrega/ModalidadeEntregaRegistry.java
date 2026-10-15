package com.loja.checkout.entrega;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ModalidadeEntregaRegistry {

    private final Map<String, ModalidadeEntrega> porCodigo;

    public ModalidadeEntregaRegistry(List<ModalidadeEntrega> modalidades) {
        this.porCodigo = modalidades.stream()
                .collect(Collectors.toUnmodifiableMap(ModalidadeEntrega::codigo, Function.identity()));
    }

    public Optional<ModalidadeEntrega> buscar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(porCodigo.get(codigo));
    }
}
