package com.loja.checkout.domain.clube;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Escolhe o nível pelo código, sem cadeia de condições. */
@Component
public class NiveisClube {

    private final Map<String, NivelClube> porCodigo;

    public NiveisClube(List<NivelClube> niveis) {
        this.porCodigo = niveis.stream()
                .collect(Collectors.toMap(NivelClube::codigo, Function.identity()));
    }

    public Optional<NivelClube> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
