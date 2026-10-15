package com.loja.checkout.domain.entrega;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Guarda as modalidades por código. Escolher a modalidade é uma busca no mapa,
 * não uma sequência de condições. Uma modalidade nova entra só por existir
 * como componente.
 */
@Component
public class RegistroModalidades {

    private final Map<String, ModalidadeEntrega> porCodigo;

    public RegistroModalidades(List<ModalidadeEntrega> modalidades) {
        this.porCodigo = modalidades.stream()
                .collect(Collectors.toUnmodifiableMap(ModalidadeEntrega::codigo, Function.identity()));
    }

    public Optional<ModalidadeEntrega> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
