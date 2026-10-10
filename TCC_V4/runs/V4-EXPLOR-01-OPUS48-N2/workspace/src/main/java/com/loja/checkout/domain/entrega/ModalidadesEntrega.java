package com.loja.checkout.domain.entrega;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Escolhe a modalidade pelo código, sem cadeia de condições. */
@Component
public class ModalidadesEntrega {

    private final Map<String, ModalidadeEntrega> porCodigo;

    public ModalidadesEntrega(List<ModalidadeEntrega> modalidades) {
        this.porCodigo = modalidades.stream()
                .collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
    }

    public Optional<ModalidadeEntrega> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
