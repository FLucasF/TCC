package com.loja.checkout.dominio.entrega;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** Reune todas as modalidades de entrega cadastradas no sistema. */
@Component
public class CatalogoEntregas {

    private final Map<String, ModalidadeEntrega> porCodigo;

    public CatalogoEntregas(List<ModalidadeEntrega> modalidades) {
        this.porCodigo = modalidades.stream().collect(java.util.stream.Collectors.toMap(
                ModalidadeEntrega::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public Optional<ModalidadeEntrega> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
