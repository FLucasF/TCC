package com.loja.checkout.service.clube;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class NivelClubeRegistry {

    private final Map<String, NivelClube> niveis;

    public NivelClubeRegistry(List<NivelClube> niveis) {
        this.niveis = niveis.stream()
                .collect(Collectors.toMap(NivelClube::getCodigo, Function.identity()));
    }

    public NivelClube buscar(String codigo) {
        return niveis.get(codigo);
    }
}
