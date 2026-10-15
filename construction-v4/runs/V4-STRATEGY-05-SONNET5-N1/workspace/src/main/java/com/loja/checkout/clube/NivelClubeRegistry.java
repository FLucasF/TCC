package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NivelClubeRegistry {

    private final Map<String, NivelClube> niveisPorCodigo;

    public NivelClubeRegistry(List<NivelClube> niveis) {
        this.niveisPorCodigo = niveis.stream()
                .collect(Collectors.toMap(NivelClube::codigo, Function.identity()));
    }

    public NivelClube buscar(String codigo) {
        return niveisPorCodigo.get(codigo);
    }
}
