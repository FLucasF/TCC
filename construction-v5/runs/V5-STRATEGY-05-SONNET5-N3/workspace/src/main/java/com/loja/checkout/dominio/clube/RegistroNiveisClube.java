package com.loja.checkout.dominio.clube;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RegistroNiveisClube {

    private final Map<String, NivelClube> niveisPorCodigo;

    public RegistroNiveisClube(List<NivelClube> niveis) {
        this.niveisPorCodigo = niveis.stream()
                .collect(Collectors.toUnmodifiableMap(NivelClube::codigo, Function.identity()));
    }

    public Optional<NivelClube> buscar(String codigo) {
        return Optional.ofNullable(niveisPorCodigo.get(codigo));
    }
}
