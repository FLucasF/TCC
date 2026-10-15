package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NiveisClubeRegistro {

    private final Map<String, NivelClube> niveisPorCodigo;

    public NiveisClubeRegistro(List<NivelClube> niveis) {
        this.niveisPorCodigo = niveis.stream()
                .collect(Collectors.toMap(NivelClube::getCodigo, Function.identity()));
    }

    public Optional<NivelClube> buscar(String codigo) {
        return Optional.ofNullable(niveisPorCodigo.get(codigo));
    }
}
